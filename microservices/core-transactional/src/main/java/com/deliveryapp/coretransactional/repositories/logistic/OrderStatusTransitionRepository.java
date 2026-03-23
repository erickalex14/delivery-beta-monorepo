package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.OrderStatusTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface OrderStatusTransitionRepository extends  JpaRepository<OrderStatusTransition, UUID> {
    // Método clave: Verifica si existe una regla que permita el cambio de estado por un rol específico
    Optional<OrderStatusTransition> findByFromStatusIdAndToStatusIdAllowedRole(
            UUID fromStatusId,
            UUID toStatusId,
            String allowedRole
    );

    @Query("SELECT t FROM OrderStatusTransition t WHERE t.fromStatus.id = :fromStatusId AND t.toStatus.id = :toStatusId AND t.allowedRole = :role")
    Optional<OrderStatusTransition> findByFromStatusIdAndToStatusIdAndAllowedRole(
            @Param("fromStatusId") UUID fromStatusId,
            @Param("toStatusId") UUID toStatusId,
            @Param("role") String allowedRole
    );

    // Método auxiliar: Devuelve todas las opciones a las que un rol puede avanzar desde el estado actual
    Page<OrderStatusTransition> findByFromStatusIdAndAllowedRole(UUID fromStatusId, String allowedRole, Pageable pageable);
}
