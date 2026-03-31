package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    // Login Tradicional: Puede ser el teléfono o el email
    private String phone;
    private String email;

    // Password: Solo requerido si no es inicio de sesión con Google
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    
}