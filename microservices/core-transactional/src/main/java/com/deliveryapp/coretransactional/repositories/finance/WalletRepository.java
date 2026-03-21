package com.deliveryapp.coretransactional.repositories.finance;

import com.deliveryapp.coretransactional.models.finance.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository  extends JpaRepository<Wallet, UUID> {

    // Método para encontrar una billetera por el ID del usuario
    Optional<Wallet> findByUserId(UUID userId);
}
