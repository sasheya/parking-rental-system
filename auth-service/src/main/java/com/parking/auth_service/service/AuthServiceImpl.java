package com.parking.auth_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.auth_service.dto.AuthResponse;
import com.parking.auth_service.dto.CurrentUserResponse;
import com.parking.auth_service.dto.LoginRequest;
import com.parking.auth_service.dto.RegisterRequest;
import com.parking.auth_service.dto.TokenRefreshRequest;
import com.parking.auth_service.dto.TokenValidateResponse;
import com.parking.auth_service.model.RefreshToken;
import com.parking.auth_service.model.RevokedToken;
import com.parking.auth_service.model.User;
import com.parking.auth_service.repository.RefreshTokenRepository;
import com.parking.auth_service.repository.RevokedTokenRepository;
import com.parking.auth_service.repository.UserRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User already exists with email: " + request.getEmail());
        }

        if (request.getRole() == com.parking.auth_service.model.Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        log.info("Registered new user with id: {} and role: {}", user.getId(), user.getRole());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = issueRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String tokenStr = request.getRefreshToken();
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        RefreshToken refreshToken = refreshTokenRepository.findByToken(hashToken(tokenStr))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token was expired or revoked. Please log in again.");
        }

        User user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = issueRefreshToken(user);
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            if (!revokedTokenRepository.existsByToken(accessToken)) {
                revokedTokenRepository.save(RevokedToken.builder()
                        .token(accessToken)
                        .revokedAt(LocalDateTime.now())
                        .build());
            }

            try {
                Long userId = jwtService.getUserIdFromToken(accessToken);
                userRepository.findById(userId).ifPresent(user -> refreshTokenRepository.deleteByUser(user));
            } catch (Exception e) {
                log.warn("Could not revoke refresh tokens on logout", e);
            }
        }
    }

    @Override
    @Transactional
    public void logout(String authHeader, String refreshToken) {
        logout(authHeader);
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(hashToken(refreshToken)).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Override
    public TokenValidateResponse validateToken(String token) {
        try {
            if (revokedTokenRepository.existsByToken(token)) {
                return TokenValidateResponse.builder().valid(false).build();
            }

            Claims claims = jwtService.validateAndParseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            return TokenValidateResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .role(role)
                    .build();
        } catch (Exception e) {
            return TokenValidateResponse.builder().valid(false).build();
        }
    }

        @Override
        @Transactional(readOnly = true)
        public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return CurrentUserResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
        }

    private String issueRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        String tokenStr = jwtService.generateRefreshToken(user);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashToken(tokenStr))
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
