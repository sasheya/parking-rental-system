package com.parking.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.user_service.dto.UserProfileDTO;
import com.parking.user_service.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getMyProfile(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        UserProfileDTO profile = userService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getProfileByUserId(@PathVariable("userId") Long userId) {
        UserProfileDTO profile = userService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDTO> updateMyProfile(Authentication authentication,
                                                           @Valid @RequestBody UserProfileDTO dto) {
        Long userId = getUserIdFromAuth(authentication);
        UserProfileDTO updated = userService.createOrUpdateProfile(userId, dto);
        return ResponseEntity.ok(updated);
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
