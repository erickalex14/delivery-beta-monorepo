package com.deliveryapp.coretransactional.dtos.response.Wallets;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
//Esta clase es un DTO (Data Transfer Object) que se utiliza para enviar la información del balance de la billetera del conductor al cliente. Contiene el ID del conductor, la moneda y el balance actual de la billetera.
//El uso de @Data de Lombok genera automáticamente los getters, setters, equals, hashCode y toString, mientras que @Builder permite construir objetos de esta clase de manera fluida.
//Al igual que en el caso de TopUpWalletRequest, esta clase se utiliza para transferir datos entre diferentes capas de la aplicación, en este caso, para enviar la información del balance de la billetera al cliente después de una consulta o una recarga exitosa.
public class WalletBalanceResponse {
    private UUID driverId;
    private String currency;
    private BigDecimal balance;
}
