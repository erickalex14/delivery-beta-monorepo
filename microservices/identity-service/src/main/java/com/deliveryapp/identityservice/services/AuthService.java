package com.deliveryapp.identityservice.services;

import com.deliveryapp.identityservice.dtos.request.LoginRequest;
import com.deliveryapp.identityservice.dtos.request.RegisterRequest;
import com.deliveryapp.identityservice.dtos.response.AuthResponse;

public interface AuthService {
    AuthResponse register (RegisterRequest registerRequest);

    AuthResponse login (LoginRequest loginRequest);

    AuthResponse refreshToken (String refreshToken);

    AuthResponse googleAuth (com.deliveryapp.identityservice.dtos.request.GoogleAuthRequest googleAuthRequest);

    void logout(String accessToken);
}
