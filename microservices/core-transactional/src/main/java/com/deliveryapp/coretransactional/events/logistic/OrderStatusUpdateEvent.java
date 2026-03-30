package com.deliveryapp.coretransactional.events.logistic;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateEvent {
    private UUID orderId;
    private UUID clientId;
    private UUID driverId;
    private String newStatus;
}
