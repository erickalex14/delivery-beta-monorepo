package com.deliveryapp.coretransactional.dtos.response.logistic;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class QuoteOrderResponse {
    private BigDecimal estimatedPrice;
    private String currency;
    private double distanceKm;
    private String type;
}