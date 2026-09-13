package com.parking.booking_service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.parking.booking_service.client.AvailabilitySlotResponse;
import com.parking.booking_service.client.ParkingServiceClient;
import com.parking.booking_service.client.ParkingSpaceResponse;
import com.parking.booking_service.dto.CreateBookingRequest;
import com.parking.booking_service.repository.BookingRepository;
import com.parking.booking_service.repository.BookingStatusHistoryRepository;
import com.parking.booking_service.service.BookingServiceImpl;

class BookingServiceImplTest {

    @Test
    void rejectsAlreadyBookedSlot() {
        ParkingServiceClient parkingClient = mock(ParkingServiceClient.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingStatusHistoryRepository historyRepository = mock(BookingStatusHistoryRepository.class);
        ParkingSpaceResponse space = ParkingSpaceResponse.builder()
                .active(true)
                .pricePerHour(new BigDecimal("5.00"))
                .availableSlots(1)
                .build();
        AvailabilitySlotResponse slot = new AvailabilitySlotResponse();
        slot.setId(3L);
        slot.setParkingSpaceId(2L);
        slot.setStartTime(LocalDateTime.now().plusHours(1));
        slot.setEndTime(slot.getStartTime().plusHours(1));
        slot.setIsBooked(true);
        when(parkingClient.getSpaceById(2L, "secret")).thenReturn(space);
        when(parkingClient.getAvailability(2L, "secret")).thenReturn(List.of(slot));

        BookingServiceImpl service = new BookingServiceImpl(bookingRepository, historyRepository, parkingClient);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "internalServiceSecret", "secret");
        CreateBookingRequest request = CreateBookingRequest.builder()
                .parkingSpaceId(2L)
                .slotId(3L)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .build();

        assertThrows(IllegalStateException.class, () -> service.createBooking(8L, request));
    }
}
