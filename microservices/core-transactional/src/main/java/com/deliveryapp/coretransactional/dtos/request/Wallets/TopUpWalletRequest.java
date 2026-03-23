package com.deliveryapp.coretransactional.dtos.request.Wallets;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TopUpWalletRequest {
    @NotNull(message = "El monto a recargar es obligatorio")
    @Min(value = 1, message = "El monto mínimo de recarga es de $1.00")
    private BigDecimal amount;
    @NotBlank(message = "La descripción del débito es obligatoria para la auditoría")
    private String description;
}
