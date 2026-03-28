package com.deliveryapp.identityservice.services.impl;

import com.deliveryapp.identityservice.dtos.request.GoogleAuthRequest;
import com.deliveryapp.identityservice.dtos.request.LoginRequest;
import com.deliveryapp.identityservice.dtos.request.RegisterRequest;
import com.deliveryapp.identityservice.dtos.response.AuthResponse;
import com.deliveryapp.identityservice.events.UserCreatedEvent;
import com.deliveryapp.identityservice.models.User;
import com.deliveryapp.identityservice.repositories.UserRepository;
import com.deliveryapp.identityservice.security.JwtService;
import com.deliveryapp.identityservice.services.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    @Override
    @Transactional // Asegura que si falla el envío del evento, no se guarde el usuario (Consistencia)
    public AuthResponse register(RegisterRequest request) {
        // 1. Validar existencia previa (Híbrido: Teléfono o Email)
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("El número de teléfono ya está registrado");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        // 2. Mapear y Encriptar contraseña con BCrypt
        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(request.getPassword() != null ?
                        passwordEncoder.encode(request.getPassword()) : null) // BCrypt para nativos
                .role(request.getRole())
                .googleId(request.getGoogleId())
                .tenantId(request.getTenantId() != null ? UUID.fromString(request.getTenantId()) : null)
                .build();

        User savedUser = userRepository.save(user);

        // 3. Publicar Evento para creación automática de Wallet
        UserCreatedEvent event = new UserCreatedEvent(
                savedUser.getId(),
                savedUser.getPhone(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        try {
            rabbitTemplate.convertAndSend("user.exchange", "user.created", event);
            System.out.println("Evento user.created enviado para el ID: " + savedUser.getId());
        } catch (Exception e) {
            throw new RuntimeException("Error al sincronizar con el módulo financiero");
        }

        // 4. Generar Tokens (Access de 15min / Refresh de 7 días)
        return generateAuthResponse(savedUser);
    }

    // Login
    @Override
    public AuthResponse login(LoginRequest request) {
        User user;

        // Logica hibrida, google o tradicional
        if (request.getGoogleId() != null) {
            user = userRepository.findByGoogleId(request.getGoogleId())
                    .orElseThrow(() -> new RuntimeException("Usuario de Google no encontrado"));
        } else {
            user = userRepository.findByPhone(request.getPhone())
                    .or(() -> userRepository.findByEmail(request.getEmail()))
                    .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

            // Validar clave con BCrypt
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Credenciales inválidas");
            }
        }
        return generateAuthResponse(user);
    }

    // Refresh Token
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // Aquí iría la lógica para validar el Refresh Token y emitir un nuevo Access Token
        return null;
    }

    // Logout
    @Override
    public void logout(String accessToken) {
        System.out.println("Token enviado a lista negra: " + accessToken);
    }

    // Google Auth (Registro y Login Automático)
    @Override
    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        try {
            // 1. Configuramos el verificador (Debes poner tu Client ID de Google Cloud aquí)
            String googleClientId = "TU_CLIENT_ID_DE_GOOGLE.apps.googleusercontent.com";
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // 2. Verificamos el token criptográficamente con Google
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new RuntimeException("Token de Google inválido o expirado");
            }

            // 3. Extraemos los datos seguros desde Google
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            // 4. Buscamos si el usuario ya existe en nuestra base de datos
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // ES UN USUARIO NUEVO -> LO REGISTRAMOS
                if (request.getPhone() == null || request.getRole() == null) {
                    throw new RuntimeException("Es un usuario nuevo. Se requiere el teléfono y el rol para completar el registro.");
                }

                user = User.builder()
                        .email(email)
                        .googleId(googleId)
                        .fullName(name)
                        .profileImage(pictureUrl)
                        .phone(request.getPhone())
                        .role(request.getRole())
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Clave aleatoria
                        .build();

                user = userRepository.save(user);

                // DISPARAMOS EL EVENTO CON TODOS SUS PARAMETROS
                rabbitTemplate.convertAndSend("user.exchange", "user.created",
                        new UserCreatedEvent(user.getId(), user.getPhone(), user.getEmail(), user.getRole()));
            } else {
                // EL USUARIO YA EXISTE -> ACTUALIZAMOS SU GOOGLE ID POR SI ACASO
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    userRepository.save(user);
                }
            }

            // 5. GENERAMOS NUESTRO PROPIO JWT REUTILIZANDO EL HELPER
            return generateAuthResponse(user);

        } catch (Exception e) {
            throw new RuntimeException("Error en la autenticación con Google: " + e.getMessage());
        }
    }

    // Helper para generar el AuthResponse con JWT
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getPhone(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getPhone());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}