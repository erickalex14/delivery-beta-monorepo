package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import com.deliveryapp.coretransactional.repositories.finance.LedgerEntryRepository;
import com.deliveryapp.coretransactional.repositories.finance.LedgerPostingRepository;
import com.deliveryapp.coretransactional.services.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryRepository entryRepository;
    private final LedgerPostingRepository postingRepository;

    // 1. Auditoría Cruzada: Buscar la entrada contable usando el ID del pedido/flete
    @Override
    public LedgerEntry getEntryByReferenceId(UUID referenceId) {
        return entryRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Asiento contable no encontrado para la referencia: " + referenceId));
    }

    // 2. Filtro de Auditoría: Buscar por tipo (Ej: Tráeme todas las recargas "TOP_UP" de hoy)
    @Override
    public Page<LedgerEntry> getEntriesByType(String type, Pageable pageable) {
        return entryRepository.findByReferenceTypeOrderByCreatedAtDesc(type, pageable);
    }

    // 3. Busca todos los movimientos de una billetera en específico
    @Override
    public Page<LedgerPosting> getPostingsByWalletId(UUID walletId, Pageable pageable) {
        return postingRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable);
    }

    // 4. Busca por el userId de la Wallet asociada
    @Override
    public Page<LedgerPosting> getPostingsByUserId(UUID userId, Pageable pageable) {
        return postingRepository.findByWalletUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // 5. Auditoría Estricta: Tráeme el débito y el crédito de un asiento contable específico
    @Override
    public Page<LedgerPosting> getPostingsByEntryId(UUID ledgerEntryId, Pageable pageable) {
        return postingRepository.findByLedgerEntryId(ledgerEntryId, pageable);
    }
}