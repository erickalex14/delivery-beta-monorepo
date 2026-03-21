package com.deliveryapp.coretransactional.controllers.finance;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import com.deliveryapp.coretransactional.services.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    // ==========================================
    // ENDPOINTS DE ENTRADAS (ASIENTOS CONTABLES)
    // ==========================================

    // 1. Búsqueda exacta (No requiere paginación)
    @GetMapping("/entries/reference/{referenceId}")
    public ResponseEntity<LedgerEntry> getEntryByReference(@PathVariable UUID referenceId) {
        return ResponseEntity.ok(ledgerService.getEntryByReferenceId(referenceId));
    }

    // 2. Búsqueda por tipo (Paginada)
    @GetMapping("/entries/type/{type}")
    public ResponseEntity<Page<LedgerEntry>> getEntriesByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ledgerService.getEntriesByType(type, pageable));
    }

    // ==========================================
    // ENDPOINTS DE MOVIMIENTOS (DÉBITOS/CRÉDITOS)
    // ==========================================

    // 3. Movimientos por ID de Billetera (Paginada)
    @GetMapping("/postings/wallet/{walletId}")
    public ResponseEntity<Page<LedgerPosting>> getPostingsByWallet(
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ledgerService.getPostingsByWalletId(walletId, pageable));
    }

    // 4. Movimientos por ID de Usuario (Conductor/Comercio) (Paginada)
    @GetMapping("/postings/user/{userId}")
    public ResponseEntity<Page<LedgerPosting>> getPostingsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ledgerService.getPostingsByUserId(userId, pageable));
    }

    // 5. Movimientos que pertenecen a un Asiento Contable específico (Paginada)
    @GetMapping("/postings/entry/{entryId}")
    public ResponseEntity<Page<LedgerPosting>> getPostingsByEntry(
            @PathVariable UUID entryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ledgerService.getPostingsByEntryId(entryId, pageable));
    }
}