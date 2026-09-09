package com.parking.auth_service.service;

import com.parking.auth_service.dto.AuthResponse;
import com.parking.auth_service.dto.CurrentUserResponse;
import com.parking.auth_service.dto.LoginRequest;
import com.parking.auth_service.dto.RegisterRequest;
import com.parking.auth_service.dto.TokenRefreshRequest;
import com.parking.auth_service.dto.TokenValidateResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(TokenRefreshRequest request);
    void logout(String authHeader);
    void logout(String authHeader, String refreshToken);
    TokenValidateResponse validateToken(String token);
    CurrentUserResponse getCurrentUser(Long userId);
}
