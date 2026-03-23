package com.deliveryapp.identityservice.repositories;

import com.deliveryapp.identityservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Búsqueda para login tradicional por teléfono (OTP principal)
    Optional<User> findByPhone(String phone);

    // Búsqueda para login tradicional por Email
    Optional<User> findByEmail(String email);

    // Búsqueda para Login/Registro con Google
    Optional<User> findByGoogleId(String googleId);

    // Verificaciones para el Registro
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}