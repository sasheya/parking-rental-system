package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.parking.parking_service.model.AvailabilitySlot;
import com.parking.parking_service.model.ParkingSpace;

class ModelLifecycleTest {

    @Test
    void availabilitySlot_onCreate_setsDefaults() throws Exception {

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .parkingSpaceId(1L)
                .build();

        Method method =
                AvailabilitySlot.class.getDeclaredMethod("onCreate");

        method.setAccessible(true);
        method.invoke(slot);

        assertNotNull(slot.getCreatedAt());
        assertFalse(slot.getIsBooked());
    }

    @Test
    void availabilitySlot_onCreate_preservesExistingBookedValue()
            throws Exception {

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .parkingSpaceId(1L)
                .isBooked(true)
                .build();

        Method method =
                AvailabilitySlot.class.getDeclaredMethod("onCreate");

        method.setAccessible(true);
        method.invoke(slot);

        assertNotNull(slot.getCreatedAt());
        assertTrue(slot.getIsBooked());
    }

    @Test
    void parkingSpace_onCreate_setsDefaults() throws Exception {

        ParkingSpace space = ParkingSpace.builder()
                .totalSlots(10)
                .build();

        Method method =
                ParkingSpace.class.getDeclaredMethod("onCreate");

        method.setAccessible(true);
        method.invoke(space);

        assertNotNull(space.getCreatedAt());
        assertNotNull(space.getUpdatedAt());
        assertTrue(space.getActive());
        assertEquals(10, space.getAvailableSlots());
    }

    @Test
    void parkingSpace_onCreate_preservesExistingValues()
            throws Exception {

        ParkingSpace space = ParkingSpace.builder()
                .totalSlots(10)
                .availableSlots(5)
                .active(false)
                .build();

        Method method =
                ParkingSpace.class.getDeclaredMethod("onCreate");

        method.setAccessible(true);
        method.invoke(space);

        assertNotNull(space.getCreatedAt());
        assertNotNull(space.getUpdatedAt());
        assertFalse(space.getActive());
        assertEquals(5, space.getAvailableSlots());
    }

    @Test
    void parkingSpace_onUpdate_updatesTimestamp() throws Exception {

        ParkingSpace space = ParkingSpace.builder()
                .build();

        Method method =
                ParkingSpace.class.getDeclaredMethod("onUpdate");

        method.setAccessible(true);
        method.invoke(space);

        assertNotNull(space.getUpdatedAt());
    }
}