package com.deliveryapp.coretransactional.models.finance;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Entidad o tabla de la base de datos para la billetera del usuario
@Entity
@Table(schema = "finance", name = "wallets")
public class Wallet {
    //Id de la billetera
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //Id del tennant
    @Column(name = "tenant_id")
    private UUID tenantId;

    //Id del usuario
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    //Moneda de la billetera
    @Column(name = "currency", length = 3)
    private String currency = "USD"; // Valor predeterminado a USD

    //Balance de la billetera
    //Bigdecimal para manejo de dinero real, es lo que te explique laplo xd
    @Column(name = "balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO; // Valor predeterminado a 0.00

    //Timestamps para auditoría
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters y setters (Consctores)

    //Seguridad, como las clases estan en private para exportarlas al exterior nesesitamos getters y setters para acceder a los atributos de la clase, es una buena practica de encapsulamiento
    //getter es un atributo que nos permite solo lectura de un atributo, es decir, nos permite obtener el valor de un atributo pero no modificarlo. Por ejemplo, si tenemos un atributo privado llamado "balance" en la clase Wallet, podríamos tener un getter llamado "getBalance()" que nos permita acceder al valor del balance sin permitirnos cambiarlo directamente.
    //setter es un atributo que nos permite modificar el valor de un atributo, es decir, nos permite establecer un nuevo valor para un atributo. Siguiendo el mismo ejemplo del atributo "balance", podríamos tener un setter llamado "setBalance(BigDecimal balance)" que nos permita cambiar el valor del balance de la billetera.

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


}
