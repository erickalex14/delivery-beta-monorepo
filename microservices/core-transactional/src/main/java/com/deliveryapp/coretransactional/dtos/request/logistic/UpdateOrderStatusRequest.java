package com.deliveryapp.coretransactional.dtos.request.logistic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "El ID del nuevo estado es obligatorio")
    private UUID targetStatusId;

    @NotNull(message = "El ID del usuario que hace el cambio es obligatorio")
    private UUID changedByUserId;

    @NotBlank(message = "El rol del usuario es obligatorio para validar permisos")
    private String userRole; // Ej: "DRIVER", "MERCHANT", "CLIENT"

    // Este es opcional porque no todos los estados exigen PIN
    private String securityPin;
}