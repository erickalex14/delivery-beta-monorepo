package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;

import java.util.List;
import java.util.UUID;

public interface LedgerService {
    // Entradas (El resumen del por qué)
    LedgerEntry getEntryByReferenceId(UUID referenceId);
    List<LedgerEntry> getEntriesByType(String type);

    // Movimientos (El detalle de la plata)
    List<LedgerPosting> getPostingsByWalletId(UUID walletId);
    List<LedgerPosting> getPostingsByUserId(UUID userId);
    List<LedgerPosting> getPostingsByEntryId(UUID ledgerEntryId);
}