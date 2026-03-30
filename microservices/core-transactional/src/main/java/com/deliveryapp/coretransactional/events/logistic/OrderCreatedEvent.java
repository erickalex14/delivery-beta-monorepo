package com.deliveryapp.coretransactional.events.logistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {
    private UUID orderId;
    private UUID clientId;
    private String type; // RIDE o DELIVERY
    private double originLat;
    private double originLng;
    private double destinationLat;
    private double destinationLng;
    private BigDecimal estimatedPrice;
}
