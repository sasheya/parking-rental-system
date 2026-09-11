package com.parking.parking_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import com.parking.common_security.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import com.parking.parking_service.dto.AvailabilitySlotDTO;
import com.parking.parking_service.service.AvailabilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Value("${internal.service-secret:}")
    private String internalServiceSecret;

    @GetMapping("/{spaceId}/slots")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getSlotsBySpaceId(@PathVariable("spaceId") Long spaceId) {
        List<AvailabilitySlotDTO> slots = availabilityService.getSlotsBySpaceId(spaceId);
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    @GetMapping("/{spaceId}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotDTO>>> getAvailability(@PathVariable Long spaceId) {
        return ResponseEntity.ok(ApiResponse.success(availabilityService.getSlotsBySpaceId(spaceId)));
    }

    @GetMapping("/internal/{spaceId}/availability")
    public ResponseEntity<List<AvailabilitySlotDTO>> getInternalAvailability(@PathVariable Long spaceId,
                                                                               @RequestHeader("X-Internal-Secret") String secret) {
        assertInternalSecret(secret);
        return ResponseEntity.ok(availabilityService.getSlotsBySpaceId(spaceId));
    }

    @PostMapping("/{spaceId}/slots")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> createSlot(@PathVariable("spaceId") Long spaceId,
                                                          Authentication authentication,
                                                          @Valid @RequestBody AvailabilitySlotDTO dto) {
        AvailabilitySlotDTO created = availabilityService.createSlot(getUserId(authentication), spaceId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PostMapping("/{spaceId}/availability")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<AvailabilitySlotDTO>> addAvailability(@PathVariable Long spaceId,
                                                               Authentication authentication,
                                                               @Valid @RequestBody AvailabilitySlotDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(availabilityService.createSlot(getUserId(authentication), spaceId, dto)));
    }

    @PutMapping("/slots/{slotId}/status")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Void>> updateSlotStatus(@PathVariable("slotId") Long slotId,
                                                 @RequestParam("isBooked") boolean isBooked,
                                                 Authentication authentication) {
        availabilityService.updateSlotStatus(getUserId(authentication), slotId, isBooked);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/slots/{slotId}/mark-booked")
    public ResponseEntity<Void> markBooked(@PathVariable("slotId") Long slotId,
                                           @RequestHeader("X-Internal-Secret") String secret) {
        assertInternalSecret(secret);
        availabilityService.updateSlotStatusInternal(slotId, true);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/slots/{slotId}/mark-unbooked")
    public ResponseEntity<Void> markUnbooked(@PathVariable("slotId") Long slotId,
                                             @RequestHeader("X-Internal-Secret") String secret) {
        assertInternalSecret(secret);
        availabilityService.updateSlotStatusInternal(slotId, false);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    private void assertInternalSecret(String secret) {
        if (internalServiceSecret.isBlank() || !internalServiceSecret.equals(secret)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid internal service credentials");
        }
    }
}
