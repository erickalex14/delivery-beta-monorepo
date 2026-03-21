package com.deliveryapp.coretransactional.repositories.finance;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}
