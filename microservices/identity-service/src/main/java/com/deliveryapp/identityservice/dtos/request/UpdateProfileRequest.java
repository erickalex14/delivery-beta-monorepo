package com.deliveryapp.identityservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "El nombre no puede estar vacio")
    private String firstName;

    @NotBlank(message = )
}
