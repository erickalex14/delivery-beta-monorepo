package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.dtos.request.logistic.UpdateOrderStatusRequest;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import jakarta.persistence.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceOrderService {

    ServiceOrder createOrder(CreateOrderRequest request);
    ServiceOrder getOrderById(UUID orderId);

    // Las búsquedas que faltaban
    Page<ServiceOrder> getOrdersByClientId(UUID clientId, Pageable pageable);
    Page<ServiceOrder> getOrdersByDriverId(UUID driverId, Pageable pageable);
    Page<ServiceOrder> getOrdersByMerchantId(UUID merchantId, Pageable pageable);
    Page<ServiceOrder> getOrdersByType(String type, Pageable pageable);
    ServiceOrder acceptOrder(UUID orderId, UUID driverId);
    // Método para cambiar el estado de un pedido y detonar el cobro si es necesario
    ServiceOrder updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

}
