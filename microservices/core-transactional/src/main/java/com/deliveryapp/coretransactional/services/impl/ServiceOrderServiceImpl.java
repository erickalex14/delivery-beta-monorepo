package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.services.ServiceOrderService;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.models.logistic.OrderSecurity;
import com.deliveryapp.coretransactional.repositories.logistic.ServiceOrderRepository;
import com.deliveryapp.coretransactional.repositories.logistic.OrderSecurityRepository;
import lombok.RequiredArgsConstructor;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    //Crear orden
    @Override
    @Transactional
    public ServiceOrder createOrder(ServiceOrder order) {
        // Guardar la orden en la base de datos
        ServiceOrder savedOrder = orderRepository.save(order);

        //Logica de seguridad: PIN 4 digitos
        String rawPin = String.format("%04d", new SecureRandom().nextInt(10000));

        OrderSecurity security = new OrderSecurity();
        security.setOrder(savedOrder);
        security.setPinHash(passwordEncoder.encode(rawPin));
        securityRepository.save(security);

        //Log para pruebas
        System.out.println("PIN generado para el pedido " + savedOrder.getId() + ": " + rawPin);
        return savedOrder;
    }

    //Obtener orden por ID
    @Override
    public ServiceOrder getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }
}
