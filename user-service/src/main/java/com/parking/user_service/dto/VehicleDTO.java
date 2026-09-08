package com.parking.user_service.dto;

import java.time.LocalDateTime;

import com.parking.user_service.model.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {
    private Long id;
    private Long userId;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    private String make;
    private String model;
    private String color;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private LocalDateTime createdAt;
}
