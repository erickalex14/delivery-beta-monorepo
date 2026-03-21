package com.deliveryapp.coretransactional.controllers.finance;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import com.deliveryapp.coretransactional.services.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    // --- ENDPOINTS DE ENTRADAS (ASIENTOS) ---

    // Auditoría Cruzada: Buscar la entrada contable usando el ID del pedido/flete
    @GetMapping("/entries/reference/{referenceId}")
    public ResponseEntity<LedgerEntry> getEntryByReference(@PathVariable UUID referenceId) {
        return ResponseEntity.ok(ledgerService.getEntryByReferenceId(referenceId));
    }

    // Filtro de Auditoría: Buscar por tipo (Ej: Tráeme todas las recargas "TOP_UP" de hoy)
    @GetMapping("/entries/type/{type}")
    public ResponseEntity<List<LedgerEntry>> getEntriesByType(@PathVariable String type) {
        return ResponseEntity.ok(ledgerService.getEntriesByType(type));
    }


    // --- ENDPOINTS DE MOVIMIENTOS (DÉBITOS/CRÉDITOS) ---

    // Busca todos los movimientos de una billetera en específico
    @GetMapping("/postings/wallet/{walletId}")
    public ResponseEntity<List<LedgerPosting>> getPostingsByWallet(@PathVariable UUID walletId) {
        return ResponseEntity.ok(ledgerService.getPostingsByWalletId(walletId));
    }

    // "Busca por el userId de la Wallet asociada, ordena por fecha descendente"
    @GetMapping("/postings/user/{userId}")
    public ResponseEntity<List<LedgerPosting>> getPostingsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ledgerService.getPostingsByUserId(userId));
    }

    // Auditoría Estricta: Tráeme el débito y el crédito de un asiento contable específico
    @GetMapping("/postings/entry/{entryId}")
    public ResponseEntity<List<LedgerPosting>> getPostingsByEntry(@PathVariable UUID entryId) {
        return ResponseEntity.ok(ledgerService.getPostingsByEntryId(entryId));
    }
}