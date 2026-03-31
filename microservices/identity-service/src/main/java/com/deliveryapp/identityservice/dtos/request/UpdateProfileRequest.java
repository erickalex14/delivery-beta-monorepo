package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String fullName;

    @Pattern(regexp = "^\\+593[0-9]{9}$", message = "El teléfono debe tener formato +593 y 9 dígitos")
    private String phone;

    private String profileImage;
}