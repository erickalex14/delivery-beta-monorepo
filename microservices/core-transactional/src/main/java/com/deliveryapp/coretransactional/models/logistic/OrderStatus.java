package com.deliveryapp.coretransactional.models.logistic;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;


@Data
@Entity
@Table(
        schema = "logistic",
        name = "order_statuses",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"code", "type"})
        }
)
public class OrderStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false)
    String code;

    @Column(name = "type", nullable = false)
    String type; //Ride o Delivery

    @Column(name = "is_initial", nullable = false)
    boolean isInitial; //Indica si es el estado inicial del pedido

    @Column(name = "is_final", nullable = false)
    boolean isFinal; //Indica si es el estado final del pedido

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

}
