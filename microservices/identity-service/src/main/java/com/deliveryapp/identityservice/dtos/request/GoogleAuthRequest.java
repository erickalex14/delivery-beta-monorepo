package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {
    @NotBlank(message = "El Token de Google es obligatorio")
    private String idToken;

    // Estos campos solo se usarán si es la PRIMERA vez que el usuario entra
    private String phone;
    private String role; // CLIENT, DRIVER o MERCHANT
}