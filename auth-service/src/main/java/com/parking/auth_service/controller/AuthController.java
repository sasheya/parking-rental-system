package com.parking.auth_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import java.time.Duration;
import org.springframework.web.bind.annotation.CookieValue;

import com.parking.auth_service.dto.AuthResponse;
import com.parking.common_security.ApiResponse;
import com.parking.auth_service.dto.LoginRequest;
import com.parking.auth_service.dto.RegisterRequest;
import com.parking.auth_service.dto.TokenRefreshRequest;
import com.parking.auth_service.dto.TokenValidateResponse;
import com.parking.auth_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return withRefreshCookie(ResponseEntity.status(HttpStatus.CREATED), response).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return withRefreshCookie(ResponseEntity.ok(), response).body(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) TokenRefreshRequest request,
            @CookieValue(value = "refreshToken", required = false) String cookieToken) {
        if (request == null) {
            request = TokenRefreshRequest.builder().refreshToken(cookieToken).build();
        } else if ((request.getRefreshToken() == null || request.getRefreshToken().isBlank()) && cookieToken != null) {
            request.setRefreshToken(cookieToken);
        }
        AuthResponse response = authService.refreshToken(request);
        return withRefreshCookie(ResponseEntity.ok(), response).body(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                       @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        authService.logout(authHeader, refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true).secure(refreshCookieSecure).path("/api/auth").maxAge(Duration.ZERO).build();
        return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(ApiResponse.success(null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidateResponse>> validate(@RequestParam("token") String token) {
        TokenValidateResponse response = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<com.parking.auth_service.dto.CurrentUserResponse>> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401)
                .body(ApiResponse.failure("UNAUTHORIZED", "Missing or invalid access token"));
        }
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser(userId)));
    }

    private org.springframework.http.ResponseEntity.BodyBuilder withRefreshCookie(
            org.springframework.http.ResponseEntity.BodyBuilder builder, AuthResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", response.getRefreshToken())
                .httpOnly(true).secure(refreshCookieSecure).path("/api/auth")
                .sameSite("Strict").maxAge(Duration.ofDays(7)).build();
        return builder.header("Set-Cookie", cookie.toString());
    }
}
