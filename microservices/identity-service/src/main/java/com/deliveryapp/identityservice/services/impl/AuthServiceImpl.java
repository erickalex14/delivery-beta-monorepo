package com.deliveryapp.identityservice.services.impl;


import com.deliveryapp.identityservice.dtos.request.LoginRequest;
import com.deliveryapp.identityservice.dtos.request.RegisterRequest;
import com.deliveryapp.identityservice.dtos.response.AuthResponse;
import com.deliveryapp.identityservice.events.UserCreatedEvent;
import com.deliveryapp.identityservice.models.User;
import com.deliveryapp.identityservice.repositories.UserRepository;
import com.deliveryapp.identityservice.security.JwtService;
import com.deliveryapp.identityservice.services.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        // Este evento será capturado por el Core Transactional para inicializar la Billetera
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
            // Log de error, pero el @Transactional hará rollback si es crítico
            throw new RuntimeException("Error al sincronizar con el módulo financiero");
        }

        // 4. Generar Tokens (Access de 15min / Refresh de 7 días)
        return generateAuthResponse(savedUser);
    }

    //Login
    @Override
    public AuthResponse login(LoginRequest request) {
        User user;

        //Logica hibrida, google o tradicional
        if (request.getGoogleId() != null) {
            user = userRepository.findByGoogleId(request.getGoogleId())
                    .orElseThrow(() -> new RuntimeException("Usuario de Google no encontrado"));
        } else {
            user = userRepository.findByPhone(request.getPhone())
                    .or(() -> userRepository.findByEmail(request.getEmail()))
                    .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));
            //Validar clave con ByCript
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Credenciales inválidas");
            }
        }
        return generateAuthResponse(user);
    }

    //Refresh Token
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // Aquí iría la lógica para validar el Refresh Token y emitir un nuevo Access Token
        return null;
    }

    //Logout
    @Override
    public void logout(String accessToken) {
        // 1. Extraer el JTI (ID del token) o el token completo
        // 2. Guardarlo en Redis como "Invalidado" hasta su fecha de expiración
        // Por ahora, como es stateless, el cliente simplemente debe borrarlo de su memoria.
        System.out.println("Token enviado a lista negra: " + accessToken);
    }

    //Helper para generar el AuthResponse con JWT
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
