package com.deliveryapp.coretransactional.controllers.logistic;

import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import com.deliveryapp.coretransactional.dtos.response.logistic.ServiceOrderResponse;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.services.ServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistic/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    // Endpoint para crear una orden de servicio
    @PostMapping
    public ResponseEntity<ServiceOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request){
        ServiceOrder savedOrder = serviceOrderService.createOrder(request);
        return ResponseEntity.ok(mapToResponse(savedOrder));
    }

    // Buscar orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> getOrderById(@PathVariable UUID id){
        ServiceOrder order = serviceOrderService.getOrderById(id);
        return ResponseEntity.ok(mapToResponse(order));
    }

    // Encontrar ordenes por cliente (CON PAGINACIÓN)
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<ServiceOrderResponse>> getOrdersByClientId(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        Pageable pageable = PageRequest.of(page, size);

        // ¡Mira qué limpio! Page ya tiene su propio .map()
        Page<ServiceOrderResponse> responses = serviceOrderService.getOrdersByClientId(clientId, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(responses);
    }

    // Encontrar ordenes por conductor (CON PAGINACIÓN)
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Page<ServiceOrderResponse>> getOrdersByDriverId(
            @PathVariable UUID driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceOrderResponse> responses = serviceOrderService.getOrdersByDriverId(driverId, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(responses);
    }

    // Encontrar ordenes por comercio (CON PAGINACIÓN)
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<Page<ServiceOrderResponse>> getOrdersByMerchantId(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceOrderResponse> responses = serviceOrderService.getOrdersByMerchantId(merchantId, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(responses);
    }

    // Encontrar por tipo de orden (CON PAGINACIÓN)
    @GetMapping("/type/{type}")
    public ResponseEntity<Page<ServiceOrderResponse>> getOrdersByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceOrderResponse> responses = serviceOrderService.getOrdersByType(type, pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(responses);
    }

    // Metodo Helper
    private ServiceOrderResponse mapToResponse(ServiceOrder order) {
        return ServiceOrderResponse.builder()
                .id(order.getId())
                .type(order.getType())
                .statusCode(order.getStatus() != null ? order.getStatus().getCode() : "UNKNOWN")
                .originLat(order.getOrigin() != null ? order.getOrigin().getY() : null)
                .originLng(order.getOrigin() != null ? order.getOrigin().getX() : null)
                .destinationLat(order.getDestination() != null ? order.getDestination().getY() : null)
                .destinationLng(order.getDestination() != null ? order.getDestination().getX() : null)
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .createdAt(order.getCreatedAt())
                .build();
    }
}