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
import java.util.Optional;
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

    // Crear orden (Con protección Idempotente)
    @Override
    @Transactional
    public ServiceOrder createOrder(CreateOrderRequest request){
        // Evita fletes duplicados por "Doble Tap" en la app
        if (request.getIdempotencyKey() != null) {
            Optional<ServiceOrder> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        ServiceOrder order = new ServiceOrder();
        order.setTenantId(request.getTenantId());
        order.setType(request.getType());
        order.setClientId(request.getClientId());
        order.setMerchantId(request.getMerchantId());
        order.setTotalAmount(request.getTotalAmount());
        order.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        order.setIdempotencyKey(request.getIdempotencyKey());

        Point origin = geometryFactory.createPoint(new Coordinate(request.getOriginLng(), request.getOriginLat()));
        Point destination = geometryFactory.createPoint(new Coordinate(request.getDestinationLng(), request.getDestinationLat()));
        origin.setSRID(4326);
        destination.setSRID(4326);
        order.setOrigin(origin);
        order.setDestination(destination);

        OrderStatus initialStatus = statusRepository.findByCodeAndType("CREATED", request.getType())
                .orElseThrow(() -> new RuntimeException("Estado inicial no encontrado"));
        order.setStatus(initialStatus);

        ServiceOrder savedOrder = orderRepository.save(order);

        String rawPin = String.format("%04d", new SecureRandom().nextInt(10000));
        OrderSecurity security = new OrderSecurity();
        security.setOrder(savedOrder);
        security.setPinHash(passwordEncoder.encode(rawPin));
        securityRepository.save(security);

        return savedOrder;
    }

    // CONDICIONES DE CARRERA (Optimistic Locking)
    @Override
    @Transactional
    public ServiceOrder acceptOrder(UUID orderId, UUID driverId) {
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (order.getDriverId() != null || !order.getStatus().getCode().equals("CREATED")) {
            throw new RuntimeException("Esta orden ya no está disponible.");
        }

        order.setDriverId(driverId);
        OrderStatus acceptedStatus = statusRepository.findByCodeAndType("ACCEPTED", order.getType())
                .orElseThrow(() -> new RuntimeException("Estado ACCEPTED no configurado"));
        order.setStatus(acceptedStatus);

        return orderRepository.save(order);
    }

    // Obtener orden por ID
    @Override
    public ServiceOrder getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }

    @Override
    public Page<ServiceOrder> getOrdersByClientId(UUID clientId, Pageable pageable) {
        return orderRepository.findByClientId(clientId, pageable);
    }

    @Override
    public Page<ServiceOrder> getOrdersByDriverId(UUID driverId, Pageable pageable) {
        return orderRepository.findByDriverId(driverId, pageable);
    }

    @Override
    public Page<ServiceOrder> getOrdersByMerchantId(UUID merchantId, Pageable pageable) {
        return orderRepository.findByMerchantId(merchantId, pageable);
    }

    @Override
    public Page<ServiceOrder> getOrdersByType(String type, Pageable pageable) {
        return orderRepository.findByType(type, pageable);
    }

    // Maquina de estados
    @Override
    @Transactional
    public ServiceOrder updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request){
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        OrderStatus currentStatus = order.getStatus();

        // Buscar por el ID del targetStatus, no por el orderId
        OrderStatus targetStatus = statusRepository.findById(request.getTargetStatusId())
                .orElseThrow(() -> new RuntimeException("Estado objetivo no encontrado"));

        // Idempotencia de estado
        if (currentStatus.getId().equals(targetStatus.getId())) {
            return order;
        }

        // Consultar si el salto es legal para el rol (State Machine Validation)
        OrderStatusTransition transition = transitionRepository
                .findByFromStatusIdAndToStatusIdAndAllowedRole(currentStatus.getId(), targetStatus.getId(), request.getUserRole())
                .orElseThrow(() -> new RuntimeException(
                        "Cambio de estado NO permitido de " + currentStatus.getCode() +
                                " a " + targetStatus.getCode() + " para el rol " + request.getUserRole()));

        // Validar PIN de seguridad si la transición lo exige
        if(transition.isRequiresPin()){
            if (request.getSecurityPin() == null || request.getSecurityPin().isBlank()) {
                throw new RuntimeException("Este cambio de estado requiere el PIN de seguridad del cliente");
            }

            OrderSecurity security = securityRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Información de seguridad no encontrada"));

            if (!passwordEncoder.matches(request.getSecurityPin(), security.getPinHash())) {
                throw new RuntimeException("PIN de seguridad incorrecto");
            }
        }

        order.setStatus(targetStatus);

        // Estampar hora de finalización
        if (targetStatus.isFinal()){
            order.setCompletedAt(LocalDateTime.now());
        }

        ServiceOrder updatedOrder = orderRepository.save(order);

        // Auditoría inmutable de estados
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus(targetStatus);
        history.setChangedBy(request.getChangedByUserId());
        historyRepository.save(history);

        // COBRO AISLADO AL FINALIZAR
        if (targetStatus.isFinal() && order.getDriverId() != null) {
            BigDecimal saasFee = new BigDecimal("0.25");

            DebitWalletRequest debitReq = new DebitWalletRequest();
            debitReq.setAmount(saasFee);
            debitReq.setDescription("Comisión logística SaaS por pedido/flete: " + order.getId());

            // Este debitWallet ya está blindado gracias al arreglo previo
            walletService.debitWallet(order.getDriverId(), debitReq);
            System.out.println("Comision SaaS cobrada por pedido/flete: " + saasFee);
        }

        return updatedOrder;
    }
}