package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import java.util.UUID;

public interface ServiceOrderService {

    //Crear una orden de servicio
    ServiceOrder createOrder(ServiceOrder order);

    //Obtener una orden de servicio por su ID
    ServiceOrder getOrderById(UUID orderId);
}
