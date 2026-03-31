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
        // 1. Validar existencia previa
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("El número de teléfono ya está registrado");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        // 2. Mapear y Encriptar contraseña con BCrypt (Ajustado a los nuevos campos)
        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null)
                .role(request.getRole())
                .authProvider("LOCAL") // Etiquetamos como usuario nativo
                .tenantId(request.getTenantId()) // Ya es un UUID desde el DTO
                .build();

        User savedUser = userRepository.save(user);

        // 3. Publicar Evento para creación automática de Wallet en el Core Transaccional
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

        // 4. Generar Tokens
        return generateAuthResponse(savedUser);
    }

    // Login (Exclusivo para credenciales manuales ahora)
    @Override
    public AuthResponse login(LoginRequest request) {
        // Buscamos por teléfono o por correo
        User user = userRepository.findByPhone(request.getPhone())
                .or(() -> userRepository.findByEmail(request.getEmail()))
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // Validar clave con BCrypt
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return generateAuthResponse(user);
    }

    // Refresh Token
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // TODO: Validar la firma del Refresh Token y generar un nuevo Access Token
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

            // 4. Buscamos si el usuario ya existe por correo o por providerId
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // ES UN USUARIO NUEVO -> LO REGISTRAMOS CON LA ESTRUCTURA CORRECTA
                if (request.getPhone() == null || request.getRole() == null) {
                    throw new RuntimeException("Es un usuario nuevo. Se requiere el teléfono y el rol para completar el registro.");
                }

                user = User.builder()
                        .email(email)
                        .providerId(googleId) // Guardamos el ID de Google aquí
                        .authProvider("GOOGLE") // Marcamos el origen
                        .fullName(name)
                        .profileImage(pictureUrl)
                        .phone(request.getPhone())
                        .role(request.getRole())
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Clave aleatoria por seguridad
                        .build();

                user = userRepository.save(user);

                // DISPARAMOS EL EVENTO
                rabbitTemplate.convertAndSend("user.exchange", "user.created",
                        new UserCreatedEvent(user.getId(), user.getPhone(), user.getEmail(), user.getRole()));
            } else {
                // EL USUARIO EXISTE -> ACTUALIZAMOS SU PROVIDER ID SI LE FALTABA
                if (user.getProviderId() == null) {
                    user.setProviderId(googleId);
                    user.setAuthProvider("GOOGLE");
                    userRepository.save(user);
                }
            }

            // 5. GENERAMOS NUESTRO PROPIO JWT REUTILIZANDO EL HELPER
            return generateAuthResponse(user);

        } catch (Exception e) {
            throw new RuntimeException("Error en la autenticación con Google: " + e.getMessage());
        }
    }

    // Helper para generar el AuthResponse con JWT (Ajustado para recibir el Objeto User)
    private AuthResponse generateAuthResponse(User user) {
        // Ahora le pasamos el objeto entero como configuramos en el JwtService
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}