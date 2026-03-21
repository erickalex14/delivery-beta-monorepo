package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrderStatusRepository extends  JpaRepository<OrderStatus, UUID> {
    Optional<OrderStatus> findByCodeAndType(String code, String type);
}
