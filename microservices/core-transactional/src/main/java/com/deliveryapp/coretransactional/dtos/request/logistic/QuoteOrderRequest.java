package com.deliveryapp.coretransactional.dtos.request.logistic;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class QuoteOrderRequest {
    @NotBlank(message = "El tipo RIDE/DELIVERY es obligatorio")
    @Pattern(regexp = "^(RIDE|DELIVERY)$", message = "Solo se permite RIDE o DELIVERY")
    private String type;

    @NotNull(message = "Longitud de origen obligatroria")
    @Min(-180) @Max(180)
    private Double originLat;

    @NotNull(message = "Longitud de origen obligatoria")
    @Min(-180) @Max(180)
    private Double originLng;

    @NotNull(message = "Latitud de destino obligatoria")
    @Min(-90) @Max(90)
    private Double destinationLat;

    @NotNull(message = "Longitud de destino obligatoria")
    @Min(-180) @Max(180)
    private Double destinationLng;
}
