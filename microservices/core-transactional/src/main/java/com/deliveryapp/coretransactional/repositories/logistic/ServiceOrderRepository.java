package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, UUID> {
    // Buscar pedidos por cercanía (PostGIS futuro)
    Page<ServiceOrder> findByClientId(UUID clientId, Pageable pageable);
    Page<ServiceOrder> findByDriverId(UUID driverId, Pageable pageable);
    Page<ServiceOrder> findByMerchantId(UUID merchantId, Pageable pageable);
    Page<ServiceOrder> findByType(String type, Pageable pageable);

    // Para validar si una orden ya fue creada (Idempotencia)
    Optional<ServiceOrder> findByIdempotencyKey(String idempotencyKey);
}