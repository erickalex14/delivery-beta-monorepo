package com.deliveryapp.coretransactional.models.logistic;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(schema = "logistic", name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // El flete o pedido al que pertenece este evento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ServiceOrder order;

    // El estado que se alcanzó en este momento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private OrderStatus status;

    // Guardamos el UUID del usuario (Conductor, Cliente, etc.) que hizo el cambio
    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    // El momento exacto del suceso, inmutable
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
