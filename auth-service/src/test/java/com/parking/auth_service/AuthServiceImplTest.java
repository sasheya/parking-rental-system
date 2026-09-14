package com.parking.auth_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.parking.auth_service.dto.AuthResponse;
import com.parking.auth_service.dto.LoginRequest;
import com.parking.auth_service.model.Role;
import com.parking.auth_service.model.User;
import com.parking.auth_service.repository.RefreshTokenRepository;
import com.parking.auth_service.repository.RevokedTokenRepository;
import com.parking.auth_service.repository.UserRepository;
import com.parking.auth_service.service.AuthServiceImpl;
import com.parking.auth_service.service.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

    @Test
    void loginReturnsTokensForValidCredentials() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RevokedTokenRepository revokedTokenRepository = mock(RevokedTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        User user = User.builder().id(7L).email("user@example.com").password("encoded")
                .fullName("Test User").role(Role.ROLE_DRIVER).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        AuthServiceImpl service = new AuthServiceImpl(userRepository, refreshTokenRepository,
                revokedTokenRepository, passwordEncoder, jwtService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "refreshExpirationMs", 604800000L);

        AuthResponse response = service.login(LoginRequest.builder()
                .email("user@example.com").password("password").build());

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(Role.ROLE_DRIVER, response.getRole());
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any());
    }
}