package com.parking.booking_service.client;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AvailabilitySlotResponse {
    private Long id;
    private Long parkingSpaceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isBooked;
}