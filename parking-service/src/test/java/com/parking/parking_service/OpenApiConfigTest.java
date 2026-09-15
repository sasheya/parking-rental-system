package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.parking.parking_service.config.OpenApiConfig;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigTest {

    @Test
    void customOpenAPI_createsCorrectConfiguration() {

        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);

        // Server
        assertNotNull(openAPI.getServers());
        assertEquals("/", openAPI.getServers().get(0).getUrl());

        // Security requirement
        assertNotNull(openAPI.getSecurity());
        assertEquals(1, openAPI.getSecurity().size());
        assertTrue(
                openAPI.getSecurity().get(0).containsKey("bearerAuth")
        );

        // Security scheme
        assertNotNull(openAPI.getComponents());
        assertNotNull(
                openAPI.getComponents()
                        .getSecuritySchemes()
                        .get("bearerAuth")
        );

        SecurityScheme scheme =
                openAPI.getComponents()
                        .getSecuritySchemes()
                        .get("bearerAuth");

        assertEquals("bearerAuth", scheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }
}
