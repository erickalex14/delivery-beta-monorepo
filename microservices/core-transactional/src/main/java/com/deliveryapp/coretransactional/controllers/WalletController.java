package com.deliveryapp.coretransactional.controllers;

import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController // Le dice a Spring que este mesero responde con JSON, no con páginas web HTML
@RequestMapping("/api/v1/wallets") // La ruta base, sie;o ideal para el Gateway
@RequiredArgsConstructor
public class WalletController {

    // El mesero necesita conocer al Chef para pasarle la orden
    private final WalletService walletService;

    // Abrimos el puerto POST para recibir recargas
    //Hacer una recarga
    @PostMapping("/{driverId}/top-up")
    public ResponseEntity<WalletBalanceResponse> topUpWallet(
            // @Valid despierta a nuestro "Cadenero" (las reglas @NotNull y @Min del DTO)
            // @RequestBody transforma el JSON de texto a un objeto de Java
            @PathVariable UUID driverId,
            @Valid @RequestBody TopUpWalletRequest request) {

        WalletBalanceResponse response = walletService.topUpWallet(driverId, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint para crear una billetera
    @PostMapping
    public  ResponseEntity<WalletBalanceResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request) {
        WalletBalanceResponse response = walletService.createWallet(request);
        return ResponseEntity.status(201).body(response); // 201 Created
    }

}