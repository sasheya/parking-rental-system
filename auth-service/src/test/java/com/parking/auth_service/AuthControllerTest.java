package com.parking.auth_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.parking.auth_service.controller.AuthController;
import com.parking.auth_service.dto.CurrentUserResponse;
import com.parking.auth_service.model.Role;
import com.parking.auth_service.service.AuthService;
import com.parking.common_security.ApiResponse;

class AuthControllerTest {

    @Test
    void meReturnsCurrentUserForAuthenticatedAccessToken() {
        AuthService authService = mock(AuthService.class);
        Authentication authentication = mock(Authentication.class);
        CurrentUserResponse currentUser = CurrentUserResponse.builder()
                .userId(3L)
                .email("user@example.com")
                .role(Role.ROLE_DRIVER)
                .build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(3L);
        when(authentication.getName()).thenReturn("3");
        when(authService.getCurrentUser(3L)).thenReturn(currentUser);

        ResponseEntity<ApiResponse<CurrentUserResponse>> response = new AuthController(authService).me(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(currentUser, response.getBody().getData());
    }
}