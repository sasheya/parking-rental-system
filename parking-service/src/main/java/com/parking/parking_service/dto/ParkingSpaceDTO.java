package com.parking.parking_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class ParkingSpaceDTO {
    private Long id;
    private Long ownerId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    private String zipCode;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePerHour;

    @NotNull(message = "Total slots is required")
    @Min(value = 1, message = "Total slots must be at least 1")
    private Integer totalSlots;

    private Integer availableSlots;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
