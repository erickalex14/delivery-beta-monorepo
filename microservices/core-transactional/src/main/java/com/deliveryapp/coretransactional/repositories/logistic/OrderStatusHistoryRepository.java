package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    //Buscar por el ID del pedido (Para la línea de tiempo del cliente en la App)
    Page<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(UUID orderId, Pageable pageable);

    //Buscar por quién hizo el cambio (Para auditar a un conductor o comercio específico)
    Page<OrderStatusHistory> findByChangedByOrderByCreatedAtDesc(UUID changedBy, Pageable pageable);

    //Buscar por el ID del estado (Ej: "Tráeme todos los pedidos que fueron CANCELADOS")
    Page<OrderStatusHistory> findByStatusIdOrderByCreatedAtDesc(UUID statusId, Pageable pageable);
}
