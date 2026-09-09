package com.parking.parking_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.parking_service.dto.AvailabilitySlotDTO;
import com.parking.parking_service.model.AvailabilitySlot;
import com.parking.parking_service.model.ParkingSpace;
import com.parking.parking_service.repository.AvailabilitySlotRepository;
import com.parking.parking_service.repository.ParkingSpaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilitySlotRepository slotRepository;
    private final ParkingSpaceRepository spaceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotDTO> getSlotsBySpaceId(Long spaceId) {
        return slotRepository.findByParkingSpaceId(spaceId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AvailabilitySlotDTO createSlot(Long ownerId, Long spaceId, AvailabilitySlotDTO dto) {
        ParkingSpace space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Parking space not found with ID: " + spaceId));
        if (!space.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("Unauthorized: parking space does not belong to the authenticated owner");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("Availability end time must be after start time");
        }

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .parkingSpaceId(space.getId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .isBooked(false)
                .build();

        slot = slotRepository.save(slot);
        log.info("Created availability slot ID: {} for space ID: {}", slot.getId(), spaceId);
        return mapToDTO(slot);
    }

    @Override
    @Transactional
    public void updateSlotStatus(Long slotId, boolean isBooked) {
        AvailabilitySlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found with ID: " + slotId));

        if (Boolean.TRUE.equals(slot.getIsBooked()) == isBooked) {
            return;
        }

        slot.setIsBooked(isBooked);
        slotRepository.save(slot);

        spaceRepository.findById(slot.getParkingSpaceId()).ifPresent(space -> {
            if (isBooked && space.getAvailableSlots() > 0) {
                space.setAvailableSlots(space.getAvailableSlots() - 1);
            } else if (!isBooked && space.getAvailableSlots() < space.getTotalSlots()) {
                space.setAvailableSlots(space.getAvailableSlots() + 1);
            }
            spaceRepository.save(space);
        });

        log.info("Updated slot ID: {} status to isBooked={}", slotId, isBooked);
    }

    private AvailabilitySlotDTO mapToDTO(AvailabilitySlot slot) {
        return AvailabilitySlotDTO.builder()
                .id(slot.getId())
                .parkingSpaceId(slot.getParkingSpaceId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .isBooked(slot.getIsBooked())
                .createdAt(slot.getCreatedAt())
                .build();
    }
}
