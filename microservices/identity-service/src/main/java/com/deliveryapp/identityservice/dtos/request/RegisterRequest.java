package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String fullName;

    // Email: Opcional inicialmente si se registra por teléfono,
    // pero obligatorio para Google Auth.
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    // Teléfono: Login principal para OTP
    // Formato ecuatoriano sugerido: +593...
    @Pattern(regexp = "^\\+593[0-9]{9}$", message = "El teléfono debe tener formato +593 y 9 dígitos")
    private String phone;

    // Password: Obligatorio para registro nativo, nulo para Google Auth.
    // Aplicaremos BCrypt en el Service como indica el informe[cite: 20].
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    // ID único proveído por Google OAuth2
    private String googleId;

    /* Roles permitidos: CLIENT, DRIVER, MERCHANT */
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "^(CLIENT|DRIVER|MERCHANT)$", message = "Rol no válido")
    private String role;

    // ID para segmentación por ciudades o países
    private UUID tenantId;
}