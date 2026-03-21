package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;

import java.util.List;
import java.util.UUID;

public interface ServiceOrderService {

    ServiceOrder createOrder(CreateOrderRequest request);
    ServiceOrder getOrderById(UUID orderId);

    // Las búsquedas que faltaban
    List<ServiceOrder> getOrdersByClientId(UUID clientId);
    List<ServiceOrder> getOrdersByDriverId(UUID driverId);
    List<ServiceOrder> getOrdersByMerchantId(UUID merchantId);
    List<ServiceOrder> getOrdersByType(String type);

}
