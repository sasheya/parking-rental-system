package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
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

import com.parking.parking_service.controller.AvailabilityController;
import com.parking.parking_service.dto.AvailabilitySlotDTO;
import com.parking.parking_service.service.AvailabilityService;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private Authentication authentication;

    private AvailabilityController controller;

    private AvailabilitySlotDTO slot;

    @BeforeEach
    void setUp() {
        controller = new AvailabilityController(availabilityService);

        ReflectionTestUtils.setField(
                controller,
                "internalServiceSecret",
                "secret"
        );

        slot = AvailabilitySlotDTO.builder()
                .id(1L)
                .parkingSpaceId(10L)
                .startTime(LocalDateTime.of(2026, 9, 16, 10, 0))
                .endTime(LocalDateTime.of(2026, 9, 16, 11, 0))
                .isBooked(false)
                .build();
    }

    @Test
    void getSlotsBySpaceId_success() {
        when(availabilityService.getSlotsBySpaceId(10L))
                .thenReturn(List.of(slot));

        ResponseEntity<?> response =
                controller.getSlotsBySpaceId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(availabilityService)
                .getSlotsBySpaceId(10L);
    }

    @Test
    void getSlotsBySpaceId_emptyList_success() {
        when(availabilityService.getSlotsBySpaceId(10L))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response =
                controller.getSlotsBySpaceId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(availabilityService)
                .getSlotsBySpaceId(10L);
    }

    @Test
    void getAvailability_success() {
        when(availabilityService.getSlotsBySpaceId(10L))
                .thenReturn(List.of(slot));

        ResponseEntity<?> response =
                controller.getAvailability(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(availabilityService)
                .getSlotsBySpaceId(10L);
    }

    @Test
    void getInternalAvailability_withValidSecret_success() {
        when(availabilityService.getSlotsBySpaceId(10L))
                .thenReturn(List.of(slot));

        ResponseEntity<List<AvailabilitySlotDTO>> response =
                controller.getInternalAvailability(
                        10L,
                        "secret"
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(slot), response.getBody());

        verify(availabilityService)
                .getSlotsBySpaceId(10L);
    }

    @Test
    void getInternalAvailability_withWrongSecret_throwsException() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.getInternalAvailability(
                        10L,
                        "wrong"
                )
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }

    @Test
    void createSlot_withLongPrincipal_success() {
        when(authentication.getPrincipal()).thenReturn(10L);

        when(availabilityService.createSlot(
                10L,
                20L,
                slot
        )).thenReturn(slot);

        ResponseEntity<?> response =
                controller.createSlot(
                        20L,
                        authentication,
                        slot
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(availabilityService)
                .createSlot(10L, 20L, slot);
    }

    @Test
    void createSlot_withStringPrincipal_success() {
        when(authentication.getPrincipal()).thenReturn("10");

        when(availabilityService.createSlot(
                10L,
                20L,
                slot
        )).thenReturn(slot);

        ResponseEntity<?> response =
                controller.createSlot(
                        20L,
                        authentication,
                        slot
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(availabilityService)
                .createSlot(10L, 20L, slot);
    }

    @Test
    void createSlot_withInvalidPrincipal_throwsException() {
        when(authentication.getPrincipal()).thenReturn("invalid");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.createSlot(
                        20L,
                        authentication,
                        slot
                )
        );
        
        assertEquals(
                "For input string: \"invalid\"",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }

    @Test
    void createSlot_withNullAuthentication_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.createSlot(
                        20L,
                        null,
                        slot
                )
        );

        assertEquals(
                "User not authenticated",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }

    @Test
    void addAvailability_success() {
        when(authentication.getPrincipal()).thenReturn(10L);

        when(availabilityService.createSlot(
                10L,
                20L,
                slot
        )).thenReturn(slot);

        ResponseEntity<?> response =
                controller.addAvailability(
                        20L,
                        authentication,
                        slot
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(availabilityService)
                .createSlot(10L, 20L, slot);
    }

    @Test
    void updateSlotStatus_successBooked() {
        when(authentication.getPrincipal()).thenReturn(10L);

        doNothing().when(availabilityService)
                .updateSlotStatus(10L, 1L, true);

        ResponseEntity<?> response =
                controller.updateSlotStatus(
                        1L,
                        true,
                        authentication
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(availabilityService)
                .updateSlotStatus(10L, 1L, true);
    }

    @Test
    void updateSlotStatus_successUnbooked() {
        when(authentication.getPrincipal()).thenReturn(10L);

        doNothing().when(availabilityService)
                .updateSlotStatus(10L, 1L, false);

        ResponseEntity<?> response =
                controller.updateSlotStatus(
                        1L,
                        false,
                        authentication
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(availabilityService)
                .updateSlotStatus(10L, 1L, false);
    }

    @Test
    void markBooked_withValidSecret_success() {
        doNothing().when(availabilityService)
                .updateSlotStatusInternal(1L, true);

        ResponseEntity<Void> response =
                controller.markBooked(1L, "secret");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(availabilityService)
                .updateSlotStatusInternal(1L, true);
    }

    @Test
    void markBooked_withWrongSecret_throwsException() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.markBooked(1L, "wrong")
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }

    @Test
    void markUnbooked_withValidSecret_success() {
        doNothing().when(availabilityService)
                .updateSlotStatusInternal(1L, false);

        ResponseEntity<Void> response =
                controller.markUnbooked(1L, "secret");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(availabilityService)
                .updateSlotStatusInternal(1L, false);
    }

    @Test
    void markUnbooked_withWrongSecret_throwsException() {
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.markUnbooked(1L, "wrong")
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }

    @Test
    void markBooked_withBlankConfiguredSecret_throwsException() {
        ReflectionTestUtils.setField(
                controller,
                "internalServiceSecret",
                ""
        );

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.markBooked(1L, "secret")
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }

    @Test
    void markUnbooked_withBlankConfiguredSecret_throwsException() {
        ReflectionTestUtils.setField(
                controller,
                "internalServiceSecret",
                ""
        );

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> controller.markUnbooked(1L, "secret")
        );

        assertEquals(
                "Invalid internal service credentials",
                exception.getMessage()
        );

        verifyNoInteractions(availabilityService);
    }
}