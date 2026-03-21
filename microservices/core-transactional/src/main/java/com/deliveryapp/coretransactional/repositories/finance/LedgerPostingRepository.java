package com.deliveryapp.coretransactional.repositories.finance;

import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface LedgerPostingRepository  extends  JpaRepository<LedgerPosting, UUID> {

    //"Busca por el userId de la Wallet asociada, ordena por fecha descendente"
    Page<LedgerPosting> findByWalletUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Busca todos los movimientos de una billetera en específico
    Page<LedgerPosting> findByWalletIdOrderByCreatedAtDesc(UUID walletId,  Pageable pageable);

    // Auditoría Estricta: Tráeme el débito y el crédito de un asiento contable específico
    Page<LedgerPosting> findByLedgerEntryId(UUID ledgerEntryId, Pageable pageable);

}
