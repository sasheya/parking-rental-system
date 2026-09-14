package com.parking.api_gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.parking.api_gateway.controller.GatewayController;
import com.parking.common_security.ApiResponse;

class GatewayControllerTest {

    @Test
    void rootReportsGatewayStatus() {
        ApiResponse<Map<String, String>> response = new GatewayController().root();

        assertEquals("api-gateway", response.getData().get("service"));
        assertEquals("UP", response.getData().get("status"));
    }
}