package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
    @NotBlank(message = "El Refresh Token es obligatorio")
    private String refreshToken;
}