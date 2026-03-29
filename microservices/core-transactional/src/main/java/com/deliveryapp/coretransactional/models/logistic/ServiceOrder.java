package com.deliveryapp.coretransactional.models.logistic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 🛠️ CORRECCIÓN 1: "logistics" con 's'
@Table(schema = "logistics", name = "service_orders")
@Data
@SoftDelete(columnName = "deleted_at")
public class ServiceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "tenant_id")
    private UUID tenantId;

    @Column(name = "type", nullable = false)
    private String type; // Ride o Delivery

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Version
    private long version;

    @Column(name = "merchant_id")
    private UUID merchantId; // Solo para delivery

    //Llave de Idempotencia
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private OrderStatus status;

    @Column(columnDefinition = "geography(Point,4326)", name = "origin")
    private Point origin;

    @Column(columnDefinition = "geography(Point,4326)", name = "destination")
    private Point destination;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}