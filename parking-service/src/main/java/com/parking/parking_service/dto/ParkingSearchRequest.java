package com.parking.parking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSearchRequest {
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private String city;
}
