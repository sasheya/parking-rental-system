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

import com.parking.parking_service.dto.AvailabilitySlotDTO;
import com.parking.parking_service.service.AvailabilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/{spaceId}/slots")
    public ResponseEntity<List<AvailabilitySlotDTO>> getSlotsBySpaceId(@PathVariable("spaceId") Long spaceId) {
        List<AvailabilitySlotDTO> slots = availabilityService.getSlotsBySpaceId(spaceId);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/{spaceId}/slots")
    public ResponseEntity<AvailabilitySlotDTO> createSlot(@PathVariable("spaceId") Long spaceId,
                                                          @Valid @RequestBody AvailabilitySlotDTO dto) {
        AvailabilitySlotDTO created = availabilityService.createSlot(spaceId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/slots/{slotId}/status")
    public ResponseEntity<Void> updateSlotStatus(@PathVariable("slotId") Long slotId,
                                                 @RequestParam("isBooked") boolean isBooked) {
        availabilityService.updateSlotStatus(slotId, isBooked);
        return ResponseEntity.noContent().build();
    }
}
