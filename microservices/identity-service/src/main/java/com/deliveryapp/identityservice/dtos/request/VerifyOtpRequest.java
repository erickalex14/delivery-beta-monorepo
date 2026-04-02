package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    private String email;

    @NotBlank(message = "El codigo OTP es obligatorio")
    private String otpCode;
}
