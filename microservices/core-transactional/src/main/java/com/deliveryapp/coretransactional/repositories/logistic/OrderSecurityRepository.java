package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.OrderSecurity;
import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrderSecurityRepository extends  JpaRepository<OrderSecurity, UUID> {
    Optional<OrderSecurity> findByOrder(ServiceOrder order);
    Optional<OrderSecurity> findByOrderId(UUID orderId);
}
