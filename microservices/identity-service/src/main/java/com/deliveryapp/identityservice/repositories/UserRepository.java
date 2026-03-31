package com.deliveryapp.identityservice.repositories;

import com.deliveryapp.identityservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderId(String providerId);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}