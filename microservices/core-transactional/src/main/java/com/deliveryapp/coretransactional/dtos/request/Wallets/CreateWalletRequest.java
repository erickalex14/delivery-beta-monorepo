package com.deliveryapp.coretransactional.dtos.request.Wallets;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateWalletRequest {
    @NotNull(message = "El ID del usuario es obligatorio para crear una billetera")
    private UUID driverId;
}
