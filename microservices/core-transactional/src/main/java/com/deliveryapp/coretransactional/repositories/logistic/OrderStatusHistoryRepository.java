package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    //Buscar por el ID del pedido (Para la línea de tiempo del cliente en la App)
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    //Buscar por quién hizo el cambio (Para auditar a un conductor o comercio específico)
    List<OrderStatusHistory> findByChangedByOrderByCreatedAtDesc(UUID changedBy);

    //Buscar por el ID del estado (Ej: "Tráeme todos los pedidos que fueron CANCELADOS")
    List<OrderStatusHistory> findByStatusIdOrderByCreatedAtDesc(UUID statusId);
}
