package com.parking.api_gateway.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.common_security.ApiResponse;

@RestController
public class GatewayController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        return ApiResponse.success(Map.of(
                "service", "api-gateway",
                "status", "UP",
                "frontend", "http://localhost:5173",
                "health", "/actuator/health"
        ));
    }
}
