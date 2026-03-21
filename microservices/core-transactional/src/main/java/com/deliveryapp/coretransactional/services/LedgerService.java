package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LedgerService {

    // 1. Auditoría Cruzada: Buscar la entrada contable usando el ID del pedido/flete
    LedgerEntry getEntryByReferenceId(UUID referenceId);

    // 2. Filtro de Auditoría: Buscar por tipo
    Page<LedgerEntry> getEntriesByType(String type, Pageable pageable);

    // 3. Busca todos los movimientos de una billetera en específico
    Page<LedgerPosting> getPostingsByWalletId(UUID walletId, Pageable pageable);

    // 4. Busca por el userId de la Wallet asociada
    Page<LedgerPosting> getPostingsByUserId(UUID userId, Pageable pageable);

    // 5. Auditoría Estricta: Tráeme el débito y el crédito de un asiento
    Page<LedgerPosting> getPostingsByEntryId(UUID ledgerEntryId, Pageable pageable);
}