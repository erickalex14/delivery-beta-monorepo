package com.deliveryapp.identityservice.controllers;

import com.deliveryapp.identityservice.dtos.request.GoogleAuthRequest;
import com.deliveryapp.identityservice.dtos.request.LoginRequest;
import com.deliveryapp.identityservice.dtos.request.RegisterRequest;
import com.deliveryapp.identityservice.dtos.response.AuthResponse; // 👈 Asegúrate de importar esto
import com.deliveryapp.identityservice.services.AuthService;
import com.deliveryapp.identityservice.services.impl.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        // Ya no hay try-catch. Devolvemos los tokens directamente.
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        // Ya no hay try-catch.
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token){
        String jwt = token.substring(7);
        authService.logout(jwt);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.googleAuth(request));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody com.deliveryapp.identityservice.dtos.request.SendOtpRequest request) {
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok("Código OTP enviado al correo (válido por 5 minutos)");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@Valid @RequestBody com.deliveryapp.identityservice.dtos.request.VerifyOtpRequest request) {
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtpCode());

        if (!isValid) {
            throw new RuntimeException("El código OTP es inválido o ha expirado");
        }

        return ResponseEntity.ok("Código verificado exitosamente");
    }
}