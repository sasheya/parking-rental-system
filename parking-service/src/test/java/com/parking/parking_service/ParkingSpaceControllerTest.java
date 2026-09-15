package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.parking.parking_service.controller.ParkingSpaceController;
import com.parking.parking_service.dto.ParkingSearchRequest;
import com.parking.parking_service.dto.ParkingSpaceDTO;
import com.parking.parking_service.service.ParkingSpaceService;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceControllerTest {

    @Mock
    private ParkingSpaceService parkingSpaceService;

    @Mock
    private Authentication authentication;

    private ParkingSpaceController controller;

    private ParkingSpaceDTO parkingSpace;

    @BeforeEach
    void setUp() {
        controller = new ParkingSpaceController(parkingSpaceService);

        ReflectionTestUtils.setField(
                controller,
                "internalServiceSecret",
                "secret"
        );

        parkingSpace = ParkingSpaceDTO.builder()
                .id(1L)
                .ownerId(10L)
                .title("Test Parking")
                .description("Test parking space")
                .address("123 Main Street")
                .city("Bhopal")
                .zipCode("462001")
                .latitude(23.2599)
                .longitude(77.4126)
                .pricePerHour(new BigDecimal("50.00"))
                .totalSlots(5)
                .availableSlots(5)
                .active(true)
                .build();
    }

    @Test
    void getAllActiveSpaces_success() {
        when(parkingSpaceService.getAllActiveSpaces())
                .thenReturn(List.of(parkingSpace));

        ResponseEntity<?> response = controller.getAllActiveSpaces();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService).getAllActiveSpaces();
    }

    @Test
    void getSpaceById_success() {
        when(parkingSpaceService.getSpaceById(1L))
                .thenReturn(parkingSpace);

        ResponseEntity<?> response = controller.getSpaceById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService).getSpaceById(1L);
    }

    @Test
    void searchSpaces_success() {
        when(parkingSpaceService.searchSpaces(any(ParkingSearchRequest.class)))
                .thenReturn(List.of(parkingSpace));

        ResponseEntity<?> response = controller.searchSpaces(
                23.2599,
                77.4126,
                5.0,
                "Bhopal"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService).searchSpaces(argThat(request ->
                Double.valueOf(23.2599).equals(request.getLatitude())
                        && Double.valueOf(77.4126).equals(request.getLongitude())
                        && Double.valueOf(5.0).equals(request.getRadiusKm())
                        && "Bhopal".equals(request.getCity())
        ));
    }

    @Test
    void searchSpaces_withNullParameters_success() {
        when(parkingSpaceService.searchSpaces(any(ParkingSearchRequest.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.searchSpaces(
                null,
                null,
                null,
                null
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(parkingSpaceService).searchSpaces(argThat(request ->
                request.getLatitude() == null
                        && request.getLongitude() == null
                        && request.getRadiusKm() == null
                        && request.getCity() == null
        ));
    }

    @Test
    void getMyListings_withLongPrincipal_success() {
        when(authentication.getPrincipal()).thenReturn(10L);
        when(parkingSpaceService.getListingsByOwnerId(10L))
                .thenReturn(List.of(parkingSpace));

        ResponseEntity<?> response =
                controller.getMyListings(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService).getListingsByOwnerId(10L);
    }

    @Test
    void getMyListings_withStringPrincipal_success() {
        when(authentication.getPrincipal()).thenReturn("10");
        when(parkingSpaceService.getListingsByOwnerId(10L))
                .thenReturn(List.of(parkingSpace));

        ResponseEntity<?> response =
                controller.getMyListings(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(parkingSpaceService).getListingsByOwnerId(10L);
    }

    @Test
    void getMyListings_withInvalidPrincipal_throwsException() {
        when(authentication.getPrincipal()).thenReturn("invalid");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getMyListings(authentication)
        );

        assertEquals(
                "Invalid authentication principal",
                exception.getMessage()
        );

        verifyNoInteractions(parkingSpaceService);
    }

    @Test
    void getMyListings_withNullAuthentication_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getMyListings(null)
        );

        assertEquals(
                "User not authenticated",
                exception.getMessage()
        );

        verifyNoInteractions(parkingSpaceService);
    }

    @Test
    void getMyListings_withNullPrincipal_throwsException() {
        when(authentication.getPrincipal()).thenReturn(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getMyListings(authentication)
        );

        assertEquals(
                "User not authenticated",
                exception.getMessage()
        );

        verifyNoInteractions(parkingSpaceService);
    }

    @Test
    void getOwnerListings_success() {
        when(parkingSpaceService.getListingsByOwnerId(10L))
                .thenReturn(List.of(parkingSpace));

        ResponseEntity<?> response =
                controller.getOwnerListings(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService).getListingsByOwnerId(10L);
    }

    @Test
    void createSpace_success() {
        when(authentication.getPrincipal()).thenReturn(10L);
        when(parkingSpaceService.createSpace(10L, parkingSpace))
                .thenReturn(parkingSpace);

        ResponseEntity<?> response =
                controller.createSpace(authentication, parkingSpace);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService)
                .createSpace(10L, parkingSpace);
    }

    @Test
    void updateSpace_success() {
        when(authentication.getPrincipal()).thenReturn(10L);
        when(parkingSpaceService.updateSpace(10L, 1L, parkingSpace))
                .thenReturn(parkingSpace);

        ResponseEntity<?> response =
                controller.updateSpace(
                        authentication,
                        1L,
                        parkingSpace
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService)
                .updateSpace(10L, 1L, parkingSpace);
    }

    @Test
    void deleteSpace_success() {
        when(authentication.getPrincipal()).thenReturn(10L);

        doNothing().when(parkingSpaceService)
                .deleteSpace(10L, 1L);

        ResponseEntity<?> response =
                controller.deleteSpace(authentication, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(parkingSpaceService)
                .deleteSpace(10L, 1L);
    }

    @Test
    void getInternalSpace_withValidSecret_success() {
        when(parkingSpaceService.getSpaceById(1L))
                .thenReturn(parkingSpace);

        ResponseEntity<ParkingSpaceDTO> response =
                controller.getInternalSpace(1L, "secret");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(parkingSpace, response.getBody());

        verify(parkingSpaceService).getSpaceById(1L);
    }

    @Test
    void getInternalSpace_withWrongSecret_throwsException() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.getInternalSpace(1L, "wrong")
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(parkingSpaceService);
    }

    @Test
    void getInternalSpace_withBlankConfiguredSecret_throwsException() {
        ReflectionTestUtils.setField(
                controller,
                "internalServiceSecret",
                ""
        );

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.getInternalSpace(1L, "secret")
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(parkingSpaceService);
    }

    @Test
    void getInternalSpace_withNullSecret_throwsException() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.getInternalSpace(1L, null)
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(parkingSpaceService);
    }
}