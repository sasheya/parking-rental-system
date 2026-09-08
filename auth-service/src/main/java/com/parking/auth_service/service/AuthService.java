package com.parking.auth_service.service;

import com.parking.auth_service.dto.AuthResponse;
import com.parking.auth_service.dto.LoginRequest;
import com.parking.auth_service.dto.RegisterRequest;
import com.parking.auth_service.dto.TokenRefreshRequest;
import com.parking.auth_service.dto.TokenValidateResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
    void logout(String authHeader);
    TokenValidateResponse validateToken(String token);
}
