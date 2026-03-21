package com.deliveryapp.coretransactional.models.logistic;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Entity
@Table(schema = "logistic", name = "order_status_transitions")
public class OrderStatusTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relación con el estado de destino
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id", nullable = false)
    private OrderStatus toStatus;

    // Qué rol está autorizado a hacer este cambio (ej. "DRIVER", "MERCHANT", "CLIENT")
    @Column(name = "allowed_role", nullable = false)
    private String allowedRole;

    // Bandera crítica: ¿Exigimos el PIN de seguridad para autorizar este salto?
    @Column(name = "requires_pin", nullable = false)
    private boolean requiresPin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
