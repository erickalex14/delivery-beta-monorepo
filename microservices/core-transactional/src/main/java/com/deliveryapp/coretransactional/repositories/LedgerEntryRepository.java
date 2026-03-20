package com.deliveryapp.coretransactional.repositories;

import com.deliveryapp.coretransactional.  models.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}
