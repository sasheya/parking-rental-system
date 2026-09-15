package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.parking.common_security.ApiResponse;
import com.parking.parking_service.exception.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgument_returnsBadRequest() {

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException("Invalid input")
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleIllegalState_returnsConflict() {

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleIllegalState(
                        new IllegalStateException("Invalid state")
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleAccessDenied_returnsForbidden() {

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDenied(
                        new AccessDeniedException("Access denied")
                );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleMalformedRequest_returnsBadRequest() {

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMalformedRequest(
                        new IllegalArgumentException("Malformed request")
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleMethodNotAllowed_returns405() {

        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException("POST");

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMethodNotAllowed(exception);

        assertEquals(
                HttpStatus.METHOD_NOT_ALLOWED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
    }

    @Test
    void handleGeneral_returnsInternalServerError() {

        ResponseEntity<ApiResponse<Void>> response =
                handler.handleGeneral(
                        new RuntimeException("Something went wrong")
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
    }
}