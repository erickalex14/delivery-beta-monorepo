package com.deliveryapp.coretransactional.repositories.finance;

import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface LedgerPostingRepository  extends  JpaRepository<LedgerPosting, UUID> {

    //"Busca por el userId de la Wallet asociada, ordena por fecha descendente"
    List<LedgerPosting> findByWalletUserIdOrderByCreatedAtDesc(UUID userId);

    // Busca todos los movimientos de una billetera en específico
    List<LedgerPosting> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    // Auditoría Estricta: Tráeme el débito y el crédito de un asiento contable específico
    List<LedgerPosting> findByLedgerEntryId(UUID ledgerEntryId);

}
