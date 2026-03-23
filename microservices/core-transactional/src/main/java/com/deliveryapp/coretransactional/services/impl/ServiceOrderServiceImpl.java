package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import com.deliveryapp.coretransactional.dtos.request.logistic.UpdateOrderStatusRequest;
import com.deliveryapp.coretransactional.models.logistic.*;
import com.deliveryapp.coretransactional.repositories.logistic.*;
import com.deliveryapp.coretransactional.services.ServiceOrderService;

import com.deliveryapp.coretransactional.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository orderRepository;
    private final OrderSecurityRepository securityRepository;
    private final OrderStatusRepository statusRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final WalletService walletService;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderStatusTransitionRepository transitionRepository;


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
        OrderStatus initialStatus = statusRepository.findByCodeAndType("CREATED", request.getType())
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

    //Maquina de estados y cobro de comision al finalizar un pedido
    @Override
    @Transactional
    public ServiceOrder updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request){
        //Buscar el pedido/orden
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        OrderStatus currentStatus = order.getStatus();

        //Buscar el estado al que se desea cambiar
        OrderStatus targetStatus = statusRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Estado objetivo no encontrado"));

        //Si ya esta en el estado por nose talvez doble click no hacer nada (indempotencia)
        if (currentStatus.getId().equals(targetStatus.getId())) {
            return order;
        }

        //Consultar si el salto es legal para el rol
        OrderStatusTransition transition = transitionRepository
                .findByFromStatusIdAndToStatusIdAllowedRole(currentStatus.getId(), targetStatus.getId(), request.getUserRole())
                .orElseThrow(() -> new RuntimeException(
                        "Cambio de estado NO permitido de " + currentStatus.getCode() +
                        " a " + targetStatus.getCode() + " para el rol " + request.getUserRole()));

        //Validar pin, si es necesario xd
        if(transition.isRequiresPin()){
            if (request.getSecurityPin() == null || request.getSecurityPin().isBlank()) {
                throw new RuntimeException("Este cambio de estado requiere un PIN de seguridad");
            }

            OrderSecurity security = securityRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Información de seguridad no encontrada para esta orden"));

            if (!passwordEncoder.matches(request.getSecurityPin(), security.getPinHash())) {
                throw new RuntimeException("PIN de seguridad incorrecto");
            }
        }

        //Actualizar el estado del pedido
        order.setStatus(targetStatus);

        //sI EL ESTADO FINAL ES Entregado o finalizado  estampar la hora
        if (targetStatus.isFinal()){
            order.setCompletedAt(LocalDateTime.now());
        }

        ServiceOrder updatedOrder = orderRepository.save(order);

        //Guardar a la wallet inmutable de auditoria
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus(targetStatus);
        history.setChangedBy(request.getChangedByUserId());
        historyRepository.save(history);

        //Si el pedido finalizo con exito, cobrar comision
        if (targetStatus.isFinal() && order.getDriverId() != null) {
            //Comision de 0.25 centavos, por el inicio, ya despues les cobramos mas XD
            BigDecimal saasFee = new BigDecimal("0.25");

            DebitWalletRequest debitReq = new DebitWalletRequest();
            debitReq.setAmount(saasFee);
            debitReq.setDescription("Comisión logística SaaS por pedido/flete: " + order.getId());
            walletService.debitWallet(order.getDriverId(), debitReq);
            System.out.println("Comision SaaS cobrada por pedido/flete: " + saasFee);
        }
        return updatedOrder;
    }

}
