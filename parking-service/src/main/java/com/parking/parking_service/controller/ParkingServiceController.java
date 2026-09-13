package com.parking.parking_service.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.common_security.ApiResponse;

@RestController
public class ParkingServiceController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        return ApiResponse.success(Map.of(
                "service", "parking-service",
                "status", "UP",
                "health", "/actuator/health"
        ));
    }
}