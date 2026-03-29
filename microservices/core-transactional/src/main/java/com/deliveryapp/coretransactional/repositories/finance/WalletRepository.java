package com.deliveryapp.coretransactional.repositories.finance;

import com.deliveryapp.coretransactional.models.finance.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository  extends JpaRepository<Wallet, UUID> {

    // Método para encontrar una billetera por el ID del usuario
    Optional<Wallet> findByUserId(UUID userId);

    // Multi-tenant: Ver todas las billeteras de una ciudad/franquicia específica
    Page<Wallet> findByTenantId(UUID tenantId, Pageable pageable);

    // Resta segura a nivel de Base de Datos (Previene Lost Updates)
    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount WHERE w.userId = :userId AND w.balance >= :amount")
    int deductBalanceSafely(@Param("userId") UUID userId, @Param("amount") BigDecimal amount);

    // Suma segura
    @Modifying
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.userId = :userId")
    int addBalanceSafely(@Param("userId") UUID userId, @Param("amount") BigDecimal amount);
}
