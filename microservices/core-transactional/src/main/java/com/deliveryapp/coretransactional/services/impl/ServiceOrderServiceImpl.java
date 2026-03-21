package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import com.deliveryapp.coretransactional.services.ServiceOrderService;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.models.logistic.OrderStatus;

import com.deliveryapp.coretransactional.models.logistic.OrderSecurity;
import com.deliveryapp.coretransactional.repositories.logistic.ServiceOrderRepository;
import com.deliveryapp.coretransactional.repositories.logistic.OrderSecurityRepository;
import com.deliveryapp.coretransactional.repositories.logistic.OrderStatusRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository orderRepository;
    private final OrderSecurityRepository securityRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final GeometryFactory geometryFactory = new GeometryFactory();


    //Crear orden
    @Override
    @Transactional
    public ServiceOrder createOrder(CreateOrderRequest request){
        //Mapear el request a la entidad
        ServiceOrder order = new ServiceOrder();
        order.setTenantId(request.getTenantId());
        order.setType(request.getType());
        order.setClientId(request.getClientId());
        order.setMerchantId(request.getMerchantId());
        order.setTotalAmount(request.getTotalAmount());
        order.setCurrency(request.getCurrency());

        //Logica Postgis
        Point origin = geometryFactory.createPoint(new Coordinate(request.getOriginLng(), request.getOriginLat()));
        Point destination = geometryFactory.createPoint(new Coordinate(request.getDestinationLng(), request.getDestinationLat()));
        origin.setSRID(4326);
        destination.setSRID(4326);
        order.setOrigin(origin);
        order.setDestination(destination);

        //Estado inicial de la orden
        OrderStatus initialStatus = orderStatusRepository.findByCodeAndType("CREATED", request.getType())
                .orElseThrow(() -> new RuntimeException("Estado inicial no encontrado"));
        order.setStatus(initialStatus);

        //Guardar la orden
        ServiceOrder savedOrder = orderRepository.save(order);

        //Generar código de seguridad para la orden
        String rawPin = String.format("%04d", new SecureRandom().nextInt(10000)); // Genera un PIN de 4 dígitos
        OrderSecurity security = new OrderSecurity();
        security.setOrder(savedOrder);
        security.setPinHash(passwordEncoder.encode(rawPin));
        securityRepository.save(security);

        //Log de prueba para mostrar el PIN generado (en producción no se haría esto)
        System.out.println("Orden creada con ID: " + savedOrder.getId() + " y PIN: " + rawPin);

        return  savedOrder;
    }

    //Obtener orden por ID
    @Override
    public ServiceOrder getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }

    //Obtener por id del cliente
    @Override
    public Page<ServiceOrder> getOrdersByClientId(UUID clientId, Pageable pageable) {
        return orderRepository.findByClientId(clientId, pageable);}

    //Obtener por id del conductor
    @Override
    public Page<ServiceOrder> getOrdersByDriverId(UUID driverId, Pageable pageable) {
        return orderRepository.findByDriverId(driverId, pageable);
    }

    //Obtener por id del comercio
    @Override
    public Page<ServiceOrder> getOrdersByMerchantId(UUID merchantId, Pageable pageable) {
        return orderRepository.findByMerchantId(merchantId, pageable);
    }

    //Obtener por tipo de orden
    @Override
    public Page<ServiceOrder> getOrdersByType(String type, Pageable pageable) {
        return orderRepository.findByType(type, pageable);}

}
