package com.deliveryapp.coretransactional.models.logistic;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(schema = "logistic", name = "order_security")
@Data
public class OrderSecurity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private ServiceOrder order;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
