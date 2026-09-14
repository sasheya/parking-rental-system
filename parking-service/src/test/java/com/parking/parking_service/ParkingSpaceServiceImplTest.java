package com.parking.parking_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.parking.parking_service.dto.ParkingSpaceDTO;
import com.parking.parking_service.model.ParkingSpace;
import com.parking.parking_service.repository.ParkingSpaceRepository;
import com.parking.parking_service.service.ParkingSpaceServiceImpl;

class ParkingSpaceServiceImplTest {

    @Test
    void createSpaceInitializesAvailableSlotsFromTotalSlots() {
        ParkingSpaceRepository repository = mock(ParkingSpaceRepository.class);
        when(repository.save(org.mockito.ArgumentMatchers.any(ParkingSpace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSpaceServiceImpl service = new ParkingSpaceServiceImpl(repository);
        ParkingSpaceDTO result = service.createSpace(4L, ParkingSpaceDTO.builder()
                .title("Central Parking")
                .address("1 Main Street")
                .city("London")
                .latitude(51.5)
                .longitude(-0.12)
                .pricePerHour(new java.math.BigDecimal("5.00"))
                .totalSlots(8)
                .build());

        assertEquals(4L, result.getOwnerId());
        assertEquals(8, result.getTotalSlots());
        assertEquals(8, result.getAvailableSlots());
        assertEquals(Boolean.TRUE, result.getActive());
    }
}