package com.deliveryapp.identityservice.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SoftDelete(columnName = "deleted_at")
@Table(schema = "identity", name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "phone, nullable = false, unique = true")
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    //ROLES
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "rating, precision = 3, scale = 2")
    private BigDecimal rating;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "google_id", unique = true)
    private String googleId; // ID único que nos da Google

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

