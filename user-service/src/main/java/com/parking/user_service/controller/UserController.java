package com.parking.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.user_service.dto.UserProfileDTO;
import com.parking.user_service.service.UserService;
import com.parking.user_service.service.VehicleService;
import com.parking.common_security.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VehicleService vehicleService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getMyProfile(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        UserProfileDTO profile = userService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfileByUserId(@PathVariable("userId") Long userId) {
        UserProfileDTO profile = userService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateMyProfile(Authentication authentication,
                                                           @Valid @RequestBody UserProfileDTO dto) {
        Long userId = getUserIdFromAuth(authentication);
        UserProfileDTO updated = userService.createOrUpdateProfile(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateProfile(@PathVariable Long userId,
                                                         Authentication authentication,
                                                         @Valid @RequestBody UserProfileDTO dto) {
        assertSameUser(userId, authentication);
        return ResponseEntity.ok(ApiResponse.success(userService.createOrUpdateProfile(userId, dto)));
    }

    @GetMapping("/{userId}/vehicles")
    public ResponseEntity<ApiResponse<java.util.List<com.parking.user_service.dto.VehicleDTO>>> getVehicles(
            @PathVariable Long userId, Authentication authentication) {
        assertSameUser(userId, authentication);
        return ResponseEntity.ok(ApiResponse.success(vehicleService.getVehiclesByUserId(userId)));
    }

    @PostMapping("/{userId}/vehicles")
    public ResponseEntity<ApiResponse<com.parking.user_service.dto.VehicleDTO>> addVehicle(
            @PathVariable Long userId, Authentication authentication,
            @Valid @RequestBody com.parking.user_service.dto.VehicleDTO dto) {
        assertSameUser(userId, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(vehicleService.addVehicle(userId, dto)));
    }

    @DeleteMapping("/{userId}/vehicles/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long userId,
                                              @PathVariable Long vehicleId,
                                              Authentication authentication) {
        assertSameUser(userId, authentication);
        vehicleService.deleteVehicle(userId, vehicleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void assertSameUser(Long userId, Authentication authentication) {
        if (!userId.equals(getUserIdFromAuth(authentication))) {
            throw new org.springframework.security.access.AccessDeniedException("User resource belongs to another account");
        }
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        } else if (authentication != null && authentication.getPrincipal() != null) {
            try {
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid authentication principal");
            }
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}
