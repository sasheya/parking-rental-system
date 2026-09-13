package com.parking.user_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.user_service.dto.VehicleDTO;
import com.parking.user_service.service.VehicleService;
import com.parking.common_security.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleDTO>>> getMyVehicles(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<VehicleDTO> vehicles = vehicleService.getVehiclesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleDTO>> addVehicle(Authentication authentication,
                                                 @Valid @RequestBody VehicleDTO dto) {
        Long userId = getUserIdFromAuth(authentication);
        VehicleDTO created = vehicleService.addVehicle(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleDTO>> updateVehicle(Authentication authentication,
                                                    @PathVariable("id") Long id,
                                                    @Valid @RequestBody VehicleDTO dto) {
        Long userId = getUserIdFromAuth(authentication);
        VehicleDTO updated = vehicleService.updateVehicle(userId, id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(Authentication authentication,
                                               @PathVariable("id") Long id) {
        Long userId = getUserIdFromAuth(authentication);
        vehicleService.deleteVehicle(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
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
