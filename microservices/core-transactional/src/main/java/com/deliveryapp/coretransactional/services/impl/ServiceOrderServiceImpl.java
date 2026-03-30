package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import com.deliveryapp.coretransactional.dtos.request.logistic.QuoteOrderRequest;
import com.deliveryapp.coretransactional.dtos.request.logistic.UpdateOrderStatusRequest;
import com.deliveryapp.coretransactional.dtos.response.logistic.QuoteOrderResponse;
import com.deliveryapp.coretransactional.events.logistic.DriverPenalizedEvent;
import com.deliveryapp.coretransactional.events.logistic.OrderCreatedEvent;
import com.deliveryapp.coretransactional.events.logistic.OrderStatusUpdateEvent;
import com.deliveryapp.coretransactional.models.logistic.*;
import com.deliveryapp.coretransactional.repositories.logistic.*;
import com.deliveryapp.coretransactional.services.ServiceOrderService;
import com.deliveryapp.coretransactional.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final RabbitTemplate rabbitTemplate;



    // Crear orden (Con protección Idempotente), y calculador de precio
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
        QuoteOrderResponse quote = calculatePrice(request.getType(), request.getDestinationLat(), request.getDestinationLng(),
                request.getOriginLat(), request.getOriginLng());
        walletService.checkBalance(request.getClientId(), quote.getEstimatedPrice());
        order.setTotalAmount(quote.getEstimatedPrice());
        order.setCurrency(quote.getCurrency());
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

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .clientId(savedOrder.getClientId())
                .type(savedOrder.getType())
                .originLat(request.getOriginLat())
                .originLng(request.getOriginLng())
                .destinationLat(request.getDestinationLat())
                .destinationLng(request.getDestinationLng())
                .estimatedPrice(savedOrder.getTotalAmount())
                .build();

        rabbitTemplate.convertAndSend("logistic.exchange", "order.created", event);
        System.out.println("Evento order.created emitido a RabbitMQ para el pedido: " + savedOrder.getId());

        return savedOrder;
    }

    // Condiciones de carrera (Optimistic Locking)
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

    // Máquina de estados
    @Override
    @Transactional
    public ServiceOrder updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request){
        ServiceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        OrderStatus currentStatus = order.getStatus();

        OrderStatus targetStatus = statusRepository.findById(request.getTargetStatusId())
                .orElseThrow(() -> new RuntimeException("Estado objetivo no encontrado"));

        if (currentStatus.getId().equals(targetStatus.getId())) return order;

        OrderStatusTransition transition = transitionRepository
                .findByFromStatusIdAndToStatusIdAndAllowedRole(currentStatus.getId(), targetStatus.getId(), request.getUserRole())
                .orElseThrow(() -> new RuntimeException("Cambio de estado NO permitido"));

        if(transition.isRequiresPin()){
            if (request.getSecurityPin() == null || request.getSecurityPin().isBlank()) {
                throw new RuntimeException("Este cambio de estado requiere un PIN de seguridad");
            }
            OrderSecurity security = securityRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Información de seguridad no encontrada"));
            if (!passwordEncoder.matches(request.getSecurityPin(), security.getPinHash())) {
                throw new RuntimeException("PIN de seguridad incorrecto");
            }
        }

        order.setStatus(targetStatus);
        if (targetStatus.isFinal()) {
            order.setCompletedAt(LocalDateTime.now());
        }

        ServiceOrder updatedOrder = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(updatedOrder);
        history.setStatus(targetStatus);
        history.setChangedBy(request.getChangedByUserId());
        historyRepository.save(history);

        // FLUJO DE DINERO Y PENALIDADES (REGLAS DE NEGOCIO)

        // A. VIAJE FINALIZADO CON ÉXITO
        if (targetStatus.getCode().equals("DELIVERED") && order.getDriverId() != null) {
            // 1. Pago al conductor
            walletService.transferFunds(
                    order.getClientId(), order.getDriverId(), order.getTotalAmount(),
                    "Pago finalizado por el pedido/flete: " + order.getId(), "ORDER_PAYMENT"
            );
            // 2. Cobro de comisión SaaS
            BigDecimal saasFee = new BigDecimal("0.25");
            DebitWalletRequest debitReq = new DebitWalletRequest();
            debitReq.setAmount(saasFee);
            debitReq.setDescription("Comisión logística SaaS por pedido/flete: " + order.getId());
            walletService.debitWallet(order.getDriverId(), debitReq);
        }

        // VIAJE CANCELADO (PENALIDADES)
        else if (targetStatus.getCode().equals("CANCELLED")) {

            // 1. Si cancela el CLIENTE y el conductor ya iba en camino
            if (request.getUserRole().equals("CLIENT") && order.getDriverId() != null) {
                // Multa del 50%
                BigDecimal penaltyAmount = order.getTotalAmount().multiply(new BigDecimal("0.50"));
                walletService.transferFunds(
                        order.getClientId(), order.getDriverId(), penaltyAmount,
                        "Compensación por cancelación de pedido: " + order.getId(), "CANCELLATION_FEE"
                );
                System.out.println("Multa del 50% aplicada al cliente: $" + penaltyAmount);
            }

            // 2. Si cancela el CONDUCTOR dejando tirado el viaje
            else if (request.getUserRole().equals("DRIVER") && order.getDriverId() != null) {
                // Disparamos evento para que el sistema lo bloquee 1 hora
                DriverPenalizedEvent penaltyEvent = DriverPenalizedEvent.builder()
                        .driverId(order.getDriverId())
                        .orderId(order.getId())
                        .penaltyMinutes(60)
                        .reason("Cancelación de viaje en curso")
                        .build();
                rabbitTemplate.convertAndSend("logistic.exchange", "driver.penalized", penaltyEvent);
                System.out.println("Evento de penalidad (60 min) emitido para el conductor: " + order.getDriverId());
            }
        }


        // DISPARO A RABBITMQ DE CAMBIO DE ESTADO

        OrderStatusUpdateEvent statusEvent = OrderStatusUpdateEvent.builder()
                .orderId(updatedOrder.getId())
                .clientId(updatedOrder.getClientId())
                .driverId(updatedOrder.getDriverId())
                .newStatus(targetStatus.getCode())
                .build();

        rabbitTemplate.convertAndSend("logistic.exchange", "order.status.updated", statusEvent);

        return updatedOrder;
    }

    //Helper cotizador, máquina de precios
    private QuoteOrderResponse calculatePrice(String type, double lat1, double lon1, double lat2, double lon2) {
        // 1. Fórmula de Haversine para obtener distancia en Kilómetros
        final int R = 6371; // Radio de la tierra en KM
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = R * c;

        // 2. Lógica de Negocio (El Tarifario)
        // Ejemplo: Base de arranque $1.25 para fletes y $1.50 para delivery. + $0.50 por Km recorrido.
        BigDecimal baseFare = type.equals("RIDE") ? new BigDecimal("1.25") : new BigDecimal("1.50");
        BigDecimal ratePerKm = new BigDecimal("0.50");

        BigDecimal estimatedPrice = baseFare.add(ratePerKm.multiply(BigDecimal.valueOf(distanceKm)))
                .setScale(2, RoundingMode.HALF_UP); // Redondeo bancario a 2 decimales

        return QuoteOrderResponse.builder()
                .estimatedPrice(estimatedPrice)
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .currency("USD")
                .type(type)
                .build();
    }

    //Endpoint para que el Frontend pregunte el precio
    public QuoteOrderResponse quoteOrder(QuoteOrderRequest request) {
        return calculatePrice(request.getType(), request.getOriginLat(), request.getOriginLng(),
                request.getDestinationLat(), request.getDestinationLng());
    }
}