package com.deliveryapp.coretransactional.dtos.request.Wallets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DebitWalletRequest {
    @NotNull(message = "El monto a debitar es obligatorio")
    @Min(value = 0, message = "El monto de débito no puede ser negativo")
    private BigDecimal amount;
    @NotBlank(message = "La descripción del débito es obligatoria para la auditoría")
    private String description;
}
