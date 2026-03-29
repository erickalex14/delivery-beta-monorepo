package com.deliveryapp.coretransactional.dtos.request.logistic;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;

@Data
public class CreateOrderRequest {
    @NotNull(message = "El tenant_id es obligatorio")
    private UUID tenantId;

    @NotBlank(message = "El tipo (RIDE/DELIVERY) es obligatorio")
    @Pattern(regexp = "^(RIDE|DELIVERY)$", message = "Solo se permite RIDE o DELIVERY")
    private String type;

    @NotNull(message = "El client_id es obligatorio")
    private UUID clientId;

    private UUID merchantId; // Opcional para RIDE

    @NotNull(message = "Coordenadas de origen obligatorias")
    @Min(value = -90) @Max(value = 90)
    private Double originLat;

    @NotNull(message = "Coordenadas de origen obligatorias")
    @Min(value = -180) @Max(value = 180)
    private Double originLng;

    @NotNull(message = "Coordenadas de destino obligatorias")
    private Double destinationLat;

    @NotNull(message = "Coordenadas de destino obligatorias")
    private Double destinationLng;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal totalAmount;

    private String currency; // Opcional, el modelo lo setea a USD por defecto

    //La llave para evitar fraude o doble-cobro por errores de red
    @NotBlank(message = "La llave de idempotencia es obligatoria")
    private String idempotencyKey;
}