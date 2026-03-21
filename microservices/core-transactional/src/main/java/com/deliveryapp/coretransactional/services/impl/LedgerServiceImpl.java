package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import com.deliveryapp.coretransactional.repositories.finance.LedgerEntryRepository;
import com.deliveryapp.coretransactional.repositories.finance.LedgerPostingRepository;
import com.deliveryapp.coretransactional.services.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerEntryRepository entryRepository;
    private final LedgerPostingRepository postingRepository;

    // Auditoría Cruzada: Buscar la entrada contable usando el ID del pedido/flete
    @Override
    public LedgerEntry getEntryByReferenceId(UUID referenceId) {
        return entryRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Asiento contable no encontrado para la referencia: " + referenceId));
    }

    // Filtro de Auditoría: Buscar por tipo (Ej: Tráeme todas las recargas "TOP_UP" de hoy)
    @Override
    public List<LedgerEntry> getEntriesByType(String type) {
        return entryRepository.findByReferenceTypeOrderByCreatedAtDesc(type);
    }

    // Busca todos los movimientos de una billetera en específico
    @Override
    public List<LedgerPosting> getPostingsByWalletId(UUID walletId) {
        return postingRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }

    // "Busca por el userId de la Wallet asociada, ordena por fecha descendente"
    @Override
    public List<LedgerPosting> getPostingsByUserId(UUID userId) {
        return postingRepository.findByWalletUserIdOrderByCreatedAtDesc(userId);
    }

    // Auditoría Estricta: Tráeme el débito y el crédito de un asiento contable específico
    @Override
    public List<LedgerPosting> getPostingsByEntryId(UUID ledgerEntryId) {
        return postingRepository.findByLedgerEntryId(ledgerEntryId);
    }
}