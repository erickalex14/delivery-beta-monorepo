package com.deliveryapp.identityservice.services.impl;


import com.deliveryapp.identityservice.dtos.request.LoginRequest;
import com.deliveryapp.identityservice.dtos.request.RegisterRequest;
import com.deliveryapp.identityservice.dtos.response.AuthResponse;
import com.deliveryapp.identityservice.models.User;
import com.deliveryapp.identityservice.repositories.UserRepository;
import com.deliveryapp.identityservice.security.JwtService;
import com.deliveryapp.identityservice.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request){
        //Validar si el usuario ya existe por email o teléfono
        if(userRepository.existsByPhone(request.getPhone())){
            throw new RuntimeException("El teléfono ya está registrado");
        }
        if (userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("El email ya está registrado");
        }
        //Crear el usuario usando ByCript para encriptar la contraseña
        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .googleId(request.getGoogleId())
                .tenantId(request.getTenantId() != null ? UUID.fromString(request.getTenantId()) : null)
                .build();
        userRepository.save(user);

        //Generar el token JWT y responder
        return generateAuthResponse(user);
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
