package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.OrderStatusTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // Método auxiliar: Devuelve todas las opciones a las que un rol puede avanzar desde el estado actual
    List<OrderStatusTransition> findByFromStatusIdAndAllowedRole(UUID fromStatusId, String allowedRole);
}
