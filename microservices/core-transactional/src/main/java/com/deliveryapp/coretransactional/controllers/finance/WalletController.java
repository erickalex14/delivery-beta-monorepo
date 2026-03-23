package com.deliveryapp.coretransactional.controllers.finance;

import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletMovementResponse;
import com.deliveryapp.coretransactional.models.finance.Wallet;
import com.deliveryapp.coretransactional.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController // Le dice a Spring que este mesero responde con JSON, no con páginas web HTML
@RequestMapping("/api/v1/finance/wallets") // La ruta base, ideal para el Gateway
@RequiredArgsConstructor
public class WalletController {

    // El mesero necesita conocer al Chef para pasarle la orden
    private final WalletService walletService;

    // Abrimos el puerto POST para recibir recargas
    // Hacer una recarga
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
    public ResponseEntity<WalletBalanceResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request) {
        WalletBalanceResponse response = walletService.createWallet(request);
        return ResponseEntity.status(201).body(response); // 201 Created
    }

    // Endpoint para debitar la billetera
    @PostMapping("/{driverId}/debit")
    public ResponseEntity<WalletBalanceResponse> debitWallet(
            @PathVariable UUID driverId,
            @Valid @RequestBody DebitWalletRequest request) {
        WalletBalanceResponse response = walletService.debitWallet(driverId, request);
        return ResponseEntity.ok(response);
    }

    // Endpoint para obtener el historial de movimientos de la billetera (PAGINADO)
    @GetMapping("/{driverId}/history")
    public ResponseEntity<Page<WalletMovementResponse>> getHistory(
            @PathVariable UUID driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<WalletMovementResponse> history = walletService.getMovementHistory(driverId, pageable);

        return ResponseEntity.ok(history);
    }

    // Auditoría Estricta: Endpoint para obtener el historial de billeteras por tenantId (PAGINADO)
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Page<Wallet>> getWalletsByTenant(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(walletService.getWalletsByTenantId(tenantId, pageable));
    }
}