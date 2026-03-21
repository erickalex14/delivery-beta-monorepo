package com.deliveryapp.coretransactional.models.logistic;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SoftDelete;
import org.locationtech.jts.geom.Point;

@Entity
@Table(schema = "logistic", name = "service_orders")
@Data
@SoftDelete(columnName = "deleted_at")
public class ServiceOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "tenant_id")
    private UUID tenantId;

    @Column(name = "type", nullable = false)
    private String type; //Ride o Delivery

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "merchant_id")
    private UUID merchantId; //Solo para delivery

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private OrderStatus status;

    @Column(columnDefinition = "geography(Point,4326)", name = "origin")
    private Point origin; //Ubicación de origen (latitud, longitud)

    @Column(columnDefinition = "geography(Point,4326)", name = "destination")
    private Point destination; //Ubicación de destino (latitud, longitud)

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount; //Monto total del servicio

    @Column(name = "currency", length = 3)
    private String currency = "USD"; //Moneda del monto total, valor predeterminado a USD

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; //Fecha y hora de finalización del servicio

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt; //Fecha y hora de cancelación del servicio
}
