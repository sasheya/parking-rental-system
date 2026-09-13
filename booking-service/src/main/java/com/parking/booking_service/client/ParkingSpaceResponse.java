package com.parking.booking_service.client;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpaceResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private String address;
    private String city;
    private BigDecimal pricePerHour;
    private Integer totalSlots;
    private Integer availableSlots;
    private Boolean active;
}
