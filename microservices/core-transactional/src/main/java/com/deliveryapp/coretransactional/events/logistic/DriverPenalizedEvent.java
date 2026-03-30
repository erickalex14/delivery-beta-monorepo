package com.deliveryapp.coretransactional.events.logistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverPenalizedEvent {
    private UUID driverId;
    private UUID orderId;
    private int penaltyMinutes; // El tiempo que estará bloqueado (ej: 60)
    private String reason;
}