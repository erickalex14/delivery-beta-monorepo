package com.deliveryapp.coretransactional.controllers.logistic;

import com.deliveryapp.coretransactional.dtos.request.logistic.CreateOrderRequest;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import com.deliveryapp.coretransactional.services.ServiceOrderService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistic/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final  ServiceOrderService serviceOrderService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    //Endpoint para crear una orden de servicio
    @PostMapping
    public ResponseEntity<ServiceOrder> createOrder(@RequestBody CreateOrderRequest request) {

    }
}
