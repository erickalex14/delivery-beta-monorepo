package com.deliveryapp.coretransactional.models.finance;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "finance", name = "ledger_postings")
@Data
public class LedgerPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_entry_id", nullable = false)
    private LedgerEntry ledgerEntry; // La entrada del libro mayor a la que pertenece esta publicación


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet; // ID de la billetera afectada

    @Column(name = "amount",precision = 10, scale = 2 ,nullable = false)
    private BigDecimal amount; // Monto de la publicación (positivo para créditos, negativo para débitos)

    @Column(name = "direction", nullable = false)
    private String direction; // Dirección de la publicación ("CREDIT" o "DEBIT")

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Fecha y hora de creación de la publicación
}
