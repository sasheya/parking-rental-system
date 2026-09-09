package com.parking.parking_service.service;

import java.util.List;

import com.parking.parking_service.dto.AvailabilitySlotDTO;

public interface AvailabilityService {
    List<AvailabilitySlotDTO> getSlotsBySpaceId(Long spaceId);
    AvailabilitySlotDTO createSlot(Long ownerId, Long spaceId, AvailabilitySlotDTO dto);
    void updateSlotStatus(Long slotId, boolean isBooked);
}
