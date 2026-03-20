package com.deliveryapp.coretransactional.repositories;

import com.deliveryapp.coretransactional.models.LedgerPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LedgerPostingRepository  extends  JpaRepository<LedgerPosting, UUID> {

}
