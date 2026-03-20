package com.deliveryapp.coretransactional.repositories;

import com.deliveryapp.coretransactional.models.LedgerPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface LedgerPostingRepository  extends  JpaRepository<LedgerPosting, UUID> {

    //"Busca por el userId de la Wallet asociada, ordena por fecha descendente"
    List<LedgerPosting> findByWalletUserIdOrderByCreatedAtDesc(UUID userId);

}
