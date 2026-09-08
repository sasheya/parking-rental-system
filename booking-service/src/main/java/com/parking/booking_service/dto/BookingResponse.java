package com.parking.booking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.parking.booking_service.model.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private Long userId;
    private Long parkingSpaceId;
    private Long vehicleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
