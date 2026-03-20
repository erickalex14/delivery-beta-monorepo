package com.deliveryapp.coretransactional.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table(schema = "finance", name = "ledger_entries")
@Entity
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_type", nullable = false)
    private String referenceType; // Tipo de referencia (e.g., "TOP_UP", "DEBIT")

    @Column(name = "reference_id")
    private UUID referenceId; // ID de la referencia (e.g., ID de la transacción)

    @Column(name = "description" , nullable = false)
    private String description; // Descripción de la entrada del libro mayor

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
