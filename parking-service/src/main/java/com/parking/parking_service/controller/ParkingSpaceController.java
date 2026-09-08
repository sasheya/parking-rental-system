package com.parking.parking_service.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parking.parking_service.dto.ParkingSearchRequest;
import com.parking.parking_service.dto.ParkingSpaceDTO;
import com.parking.parking_service.service.ParkingSpaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;

    @GetMapping
    public ResponseEntity<List<ParkingSpaceDTO>> getAllActiveSpaces() {
        List<ParkingSpaceDTO> spaces = parkingSpaceService.getAllActiveSpaces();
        return ResponseEntity.ok(spaces);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSpaceDTO> getSpaceById(@PathVariable("id") Long id) {
        ParkingSpaceDTO space = parkingSpaceService.getSpaceById(id);
        return ResponseEntity.ok(space);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ParkingSpaceDTO>> searchSpaces(
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "city", required = false) String city) {

        ParkingSearchRequest request = ParkingSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(radiusKm)
                .city(city)
                .build();

        List<ParkingSpaceDTO> results = parkingSpaceService.searchSpaces(request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/owner/my-listings")
    public ResponseEntity<List<ParkingSpaceDTO>> getMyListings(Authentication authentication) {
        Long ownerId = getUserIdFromAuth(authentication);
        List<ParkingSpaceDTO> listings = parkingSpaceService.getListingsByOwnerId(ownerId);
        return ResponseEntity.ok(listings);
    }

    @PostMapping
    public ResponseEntity<ParkingSpaceDTO> createSpace(Authentication authentication,
                                                       @Valid @RequestBody ParkingSpaceDTO dto) {
        Long ownerId = getUserIdFromAuth(authentication);
        ParkingSpaceDTO created = parkingSpaceService.createSpace(ownerId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSpaceDTO> updateSpace(Authentication authentication,
                                                       @PathVariable("id") Long id,
                                                       @Valid @RequestBody ParkingSpaceDTO dto) {
        Long ownerId = getUserIdFromAuth(authentication);
        ParkingSpaceDTO updated = parkingSpaceService.updateSpace(ownerId, id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpace(Authentication authentication,
                                             @PathVariable("id") Long id) {
        Long ownerId = getUserIdFromAuth(authentication);
        parkingSpaceService.deleteSpace(ownerId, id);
        return ResponseEntity.noContent().build();
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
