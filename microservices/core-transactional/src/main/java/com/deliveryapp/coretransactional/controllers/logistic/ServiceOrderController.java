package com.deliveryapp.coretransactional.controllers.logistic;

import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import com.deliveryapp.coretransactional.dtos.response.logistic.ServiceOrderResponse;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.services.ServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistic/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final  ServiceOrderService serviceOrderService;

    // Endpoint para crear una orden de servicio
    @PostMapping
    public ResponseEntity<ServiceOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request){
        ServiceOrder savedOrder = serviceOrderService.createOrder(request);
        return ResponseEntity.ok(mapToResponse(savedOrder));
    }

    //Buscar orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrderResponse> getOrderById(@PathVariable UUID id){
        ServiceOrder order = serviceOrderService.getOrderById(id);
        return ResponseEntity.ok(mapToResponse(order));
    }

    // Encontrar ordenes por cliente
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ServiceOrderResponse>> getOrdersByClientId(@PathVariable UUID clientId){
        List<ServiceOrderResponse> responses = serviceOrderService.getOrdersByClientId(clientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    //encontrar ordenes por conductor
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<ServiceOrderResponse>> getOrdersByDriverId(@PathVariable UUID driverId){
        List<ServiceOrderResponse> responses = serviceOrderService.getOrdersByDriverId(driverId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    //encontrar ordenes por comercio
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<ServiceOrderResponse>> getOrdersByMerchantId(@PathVariable UUID merchantId){
        List<ServiceOrderResponse> responses = serviceOrderService.getOrdersByMerchantId(merchantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    //encontar por tipo de orden
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ServiceOrderResponse>> getOrdersByType(@PathVariable String type){
        List<ServiceOrderResponse> responses = serviceOrderService.getOrdersByType(type)
                .stream()
                .map(this::mapToResponse)
                .toList();
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
