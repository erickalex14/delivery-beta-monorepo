package com.deliveryapp.identityservice.services.impl;

import com.deliveryapp.identityservice.dtos.request.GoogleAuthRequest;
import com.deliveryapp.identityservice.dtos.request.LoginRequest;
import com.deliveryapp.identityservice.dtos.request.RegisterRequest;
import com.deliveryapp.identityservice.dtos.response.AuthResponse;
import com.deliveryapp.identityservice.events.UserCreatedEvent;
import com.deliveryapp.identityservice.models.User;
import com.deliveryapp.identityservice.repositories.UserRepository;
import com.deliveryapp.identityservice.repositories.RefreshTokenRepository; // 🛡️ Import de Base de Datos
import com.deliveryapp.identityservice.security.JwtBlacklistService;
import com.deliveryapp.identityservice.security.JwtService;
import com.deliveryapp.identityservice.services.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RabbitTemplate rabbitTemplate;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtBlacklistService jwtBlacklistService;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenDurationMs;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenDurationMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("El número de teléfono ya está registrado");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null)
                .role(request.getRole())
                .authProvider("LOCAL")
                .tenantId(request.getTenantId())
                .build();

        User savedUser = userRepository.save(user);

        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getPhone(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        try {
            rabbitTemplate.convertAndSend("user.exchange", "user.created", event);
        } catch (Exception e) {
            throw new RuntimeException("Error al sincronizar con el módulo financiero");
        }

        return generateAuthResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .or(() -> userRepository.findByEmail(request.getEmail()))
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return generateAuthResponse(user);
    }

    // Refresh Token
    @Override
    @Transactional
    public AuthResponse refreshToken(String requestRefreshToken) {
        com.deliveryapp.identityservice.models.RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token no encontrado en la base de datos"));

        if (refreshTokenEntity.getExpiryDate().compareTo(java.time.Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new RuntimeException("El Refresh Token ha expirado. Por favor, inicie sesión nuevamente.");
        }

        User user = refreshTokenEntity.getUser();
        return generateAuthResponse(user);
    }

    @Override
    public void logout(String accessToken) {

        // En un caso real más avanzado, aquí desencriptaríamos el token para
        // ver exactamente cuántos milisegundos le quedan.
        // Por ahora, para asegurar, lo bloqueamos por el tiempo máximo que dura un Access Token (15 min).
        jwtBlacklistService.addToBlacklist(accessToken, accessTokenDurationMs);

        System.out.println("Access Token bloqueado en Redis exitosamente: " + accessToken);
    }

    @Override
    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new RuntimeException("Token de Google inválido o expirado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                if (request.getPhone() == null || request.getRole() == null) {
                    throw new RuntimeException("Es un usuario nuevo. Se requiere el teléfono y el rol para completar el registro.");
                }

                user = User.builder()
                        .email(email)
                        .providerId(googleId)
                        .authProvider("GOOGLE")
                        .fullName(name)
                        .profileImage(pictureUrl)
                        .phone(request.getPhone())
                        .role(request.getRole())
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .build();

                user = userRepository.save(user);

                rabbitTemplate.convertAndSend("user.exchange", "user.created",
                        new UserCreatedEvent(user.getId(), user.getPhone(), user.getEmail(), user.getRole()));
            } else {
                if (user.getProviderId() == null) {
                    user.setProviderId(googleId);
                    user.setAuthProvider("GOOGLE");
                    userRepository.save(user);
                }
            }

            return generateAuthResponse(user);

        } catch (Exception e) {
            throw new RuntimeException("Error en la autenticación con Google: " + e.getMessage());
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.deleteByUser(user);

        com.deliveryapp.identityservice.models.RefreshToken rt = com.deliveryapp.identityservice.models.RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(java.time.Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        refreshTokenRepository.save(rt);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}