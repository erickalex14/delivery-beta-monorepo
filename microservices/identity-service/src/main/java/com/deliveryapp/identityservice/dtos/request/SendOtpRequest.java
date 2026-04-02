package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    private String email;
}
