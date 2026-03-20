package com.deliveryapp.coretransactional.dtos.response.Wallets;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WalletMovementResponse {
    private String description;
    private BigDecimal amount;
    private String direction; //CREDIT o DEBIT
    private LocalDateTime date;
}
