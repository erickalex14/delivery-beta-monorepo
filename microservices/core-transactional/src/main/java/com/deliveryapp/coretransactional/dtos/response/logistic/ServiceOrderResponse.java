package com.deliveryapp.coretransactional.dtos.response.logistic;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
public class ServiceOrderResponse {
    private UUID id;
    private String type;
    private String statusCode; // Ejemplo: "CREATED"
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime createdAt;
}
