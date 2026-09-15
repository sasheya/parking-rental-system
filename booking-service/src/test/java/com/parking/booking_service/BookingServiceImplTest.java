package com.parking.booking_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import com.parking.booking_service.dto.BookingResponse;
import com.parking.booking_service.client.AvailabilitySlotResponse;
import com.parking.booking_service.client.ParkingServiceClient;
import com.parking.booking_service.client.ParkingSpaceResponse;
import com.parking.booking_service.dto.BookingStatusUpdateRequest;
import com.parking.booking_service.dto.CreateBookingRequest;
import com.parking.booking_service.model.Booking;
import com.parking.booking_service.model.BookingStatus;
import com.parking.booking_service.repository.BookingRepository;
import com.parking.booking_service.repository.BookingStatusHistoryRepository;
import com.parking.booking_service.service.BookingServiceImpl;

class BookingServiceImplTest {

    private BookingRepository bookingRepository;
    private BookingStatusHistoryRepository historyRepository;
    private ParkingServiceClient parkingServiceClient;
    private BookingServiceImpl service;

    private final String secret = "secret";

    private final LocalDateTime start =
            LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);

    private final LocalDateTime end =
            start.plusHours(2);

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        historyRepository = mock(BookingStatusHistoryRepository.class);
        parkingServiceClient = mock(ParkingServiceClient.class);

        service = new BookingServiceImpl(
                bookingRepository,
                historyRepository,
                parkingServiceClient);

        // @Value field is not populated when using Mockito directly.
        org.springframework.test.util.ReflectionTestUtils.setField(
                service,
                "internalServiceSecret",
                secret);
    }

    private CreateBookingRequest createRequest() {
        return CreateBookingRequest.builder()
                .parkingSpaceId(1L)
                .slotId(100L)
                .vehicleId(50L)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private ParkingSpaceResponse parkingSpace() {
        return ParkingSpaceResponse.builder()
                .id(1L)
                .ownerId(20L)
                .title("Central Parking")
                .address("1 Main Street")
                .city("London")
                .pricePerHour(new BigDecimal("10.00"))
                .totalSlots(10)
                .availableSlots(5)
                .active(true)
                .build();
    }

    private AvailabilitySlotResponse availabilitySlot() {
        AvailabilitySlotResponse slot =
                new AvailabilitySlotResponse();

        slot.setId(100L);
        slot.setParkingSpaceId(1L);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setIsBooked(false);

        return slot;
    }

    private Booking booking() {
        return Booking.builder()
                .id(1L)
                .userId(10L)
                .parkingSpaceId(1L)
                .slotId(100L)
                .vehicleId(50L)
                .startTime(start)
                .endTime(end)
                .totalAmount(new BigDecimal("20.00"))
                .status(BookingStatus.PENDING_PAYMENT)
                .build();
    }

    private void mockSuccessfulParkingChecks() {
        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(parkingSpace());

        when(parkingServiceClient.getAvailability(1L, secret))
                .thenReturn(List.of(availabilitySlot()));
    }

    // =========================================================
    // createBooking - success
    // =========================================================

    @Test
    void createBooking_success() {
        mockSuccessfulParkingChecks();

        Booking saved = booking();
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking b = invocation.getArgument(0);
                    b.setId(1L);
                    return b;
                });

        CreateBookingRequest request = createRequest();

        BookingResponse result =
                service.createBooking(10L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getUserId());
        assertEquals(1L, result.getParkingSpaceId());
        assertEquals(100L, result.getSlotId());
        assertEquals(50L, result.getVehicleId());
        assertEquals(new BigDecimal("20.00"), result.getTotalAmount());
        assertEquals(
                BookingStatus.PENDING_PAYMENT,
                result.getStatus());

        verify(parkingServiceClient)
                .markBooked(100L, secret);

        verify(bookingRepository)
                .save(any(Booking.class));

        verify(historyRepository)
                .save(any());

        verify(parkingServiceClient, never())
                .markUnbooked(anyLong(), anyString());
    }

    @Test
    void createBooking_roundsUpPartialHour() {
        LocalDateTime partialEnd =
                start.plusMinutes(61);

        CreateBookingRequest request =
                createRequest();
        request.setEndTime(partialEnd);

        AvailabilitySlotResponse slot =
                availabilitySlot();
        slot.setEndTime(partialEnd);

        ParkingSpaceResponse space =
                parkingSpace();
        space.setPricePerHour(new BigDecimal("10.00"));

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(space);

        when(parkingServiceClient.getAvailability(1L, secret))
                .thenReturn(List.of(slot));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking b = invocation.getArgument(0);
                    b.setId(2L);
                    return b;
                });

        BookingResponse result =
                service.createBooking(10L, request);

        // 61 minutes -> 2 hours -> 20.00
        assertEquals(
                new BigDecimal("20.00"),
                result.getTotalAmount());
    }

    @Test
    void createBooking_usesMinimumOneHour() {
        LocalDateTime shortEnd =
                start.plusMinutes(30);

        CreateBookingRequest request =
                createRequest();
        request.setEndTime(shortEnd);

        AvailabilitySlotResponse slot =
                availabilitySlot();
        slot.setEndTime(shortEnd);

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(parkingSpace());

        when(parkingServiceClient.getAvailability(1L, secret))
                .thenReturn(List.of(slot));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking b = invocation.getArgument(0);
                    b.setId(3L);
                    return b;
                });

        BookingResponse result =
                service.createBooking(10L, request);

        assertEquals(
                new BigDecimal("10.00"),
                result.getTotalAmount());
    }

    // =========================================================
    // createBooking - validation
    // =========================================================

    @Test
    void createBooking_throwsWhenEndTimeIsNotAfterStartTime() {
        CreateBookingRequest request = createRequest();
        request.setEndTime(start);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createBooking(10L, request));

        assertEquals(
                "End time must be after start time",
                exception.getMessage());

        verifyNoInteractions(parkingServiceClient);
    }

    @Test
    void createBooking_throwsWhenStartTimeIsInPast() {
        CreateBookingRequest request = createRequest();

        request.setStartTime(
                LocalDateTime.now().minusMinutes(10));
        request.setEndTime(
                LocalDateTime.now().plusHours(1));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createBooking(10L, request));

        assertEquals(
                "Start time cannot be in the past",
                exception.getMessage());

        verifyNoInteractions(parkingServiceClient);
    }

    @Test
    void createBooking_throwsWhenParkingServiceUnavailable() {
        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenThrow(new RuntimeException("Connection failed"));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking service is unavailable; booking cannot be created",
                exception.getMessage());

        verify(bookingRepository, never())
                .save(any());
    }

    @Test
    void createBooking_throwsWhenParkingSpaceIsNull() {
        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking space is not available",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenParkingSpaceInactive() {
        ParkingSpaceResponse space =
                parkingSpace();
        space.setActive(false);

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(space);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking space is not available",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenPriceIsNull() {
        ParkingSpaceResponse space =
                parkingSpace();
        space.setPricePerHour(null);

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(space);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking space has no valid price",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenPriceIsNegative() {
        ParkingSpaceResponse space =
                parkingSpace();
        space.setPricePerHour(new BigDecimal("-5.00"));

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(space);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking space has no valid price",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenNoAvailableSlots() {
        ParkingSpaceResponse space =
                parkingSpace();
        space.setAvailableSlots(0);

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(space);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking space has no available slots",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenAvailableSlotsIsNull() {
        ParkingSpaceResponse space =
                parkingSpace();
        space.setAvailableSlots(null);

        when(parkingServiceClient.getSpaceById(1L, secret))
                .thenReturn(space);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Parking space has no available slots",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenSlotDoesNotBelongToSpace() {
        mockSuccessfulParkingChecks();

        CreateBookingRequest request =
                createRequest();
        request.setSlotId(999L);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createBooking(10L, request));

        assertEquals(
                "Availability slot does not belong to the parking space",
                exception.getMessage());

        verify(parkingServiceClient, never())
                .markBooked(anyLong(), anyString());
    }

    @Test
    void createBooking_throwsWhenSlotAlreadyBooked() {
        mockSuccessfulParkingChecks();

        AvailabilitySlotResponse slot =
                availabilitySlot();
        slot.setIsBooked(true);

        when(parkingServiceClient.getAvailability(1L, secret))
                .thenReturn(List.of(slot));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Availability slot is already booked",
                exception.getMessage());
    }

    @Test
    void createBooking_throwsWhenBookingTimeDoesNotMatchSlot() {
        mockSuccessfulParkingChecks();

        AvailabilitySlotResponse slot =
                availabilitySlot();
        slot.setEndTime(end.plusHours(1));

        when(parkingServiceClient.getAvailability(1L, secret))
                .thenReturn(List.of(slot));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertEquals(
                "Booking time must match the selected availability slot",
                exception.getMessage());

        verify(parkingServiceClient, never())
                .markBooked(anyLong(), anyString());
    }

    // =========================================================
    // createBooking - repository failure / rollback
    // =========================================================

    @Test
    void createBooking_unbooksSlotWhenRepositorySaveFails() {
        mockSuccessfulParkingChecks();

        RuntimeException databaseException =
                new RuntimeException("Database failure");

        when(bookingRepository.save(any(Booking.class)))
                .thenThrow(databaseException);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> service.createBooking(
                                10L,
                                createRequest()));

        assertSame(databaseException, exception);

        verify(parkingServiceClient)
                .markBooked(100L, secret);

        verify(parkingServiceClient)
                .markUnbooked(100L, secret);

        verify(historyRepository, never())
                .save(any());
    }

    // =========================================================
    // getBookingById
    // =========================================================

    @Test
    void getBookingById_returnsBooking() {
        Booking booking = booking();

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        BookingResponse result =
                service.getBookingById(1L);

        assertEquals(1L, result.getId());
        assertEquals(10L, result.getUserId());
        assertEquals(1L, result.getParkingSpaceId());
        assertEquals(100L, result.getSlotId());
        assertEquals(
                new BigDecimal("20.00"),
                result.getTotalAmount());
        assertEquals(
                BookingStatus.PENDING_PAYMENT,
                result.getStatus());
    }

    @Test
    void getBookingById_throwsWhenNotFound() {
        when(bookingRepository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.getBookingById(99L));

        assertEquals(
                "Booking not found with ID: 99",
                exception.getMessage());
    }

    // =========================================================
    // getBookingByIdForUser
    // =========================================================

    @Test
    void getBookingByIdForUser_returnsOwnedBooking() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking()));

        BookingResponse result =
                service.getBookingByIdForUser(10L, 1L);

        assertEquals(10L, result.getUserId());
    }

    @Test
    void getBookingByIdForUser_throwsWhenDifferentUser() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking()));

        AccessDeniedException exception =
                assertThrows(
                        AccessDeniedException.class,
                        () -> service.getBookingByIdForUser(
                                999L,
                                1L));

        assertEquals(
                "Booking belongs to another renter",
                exception.getMessage());
    }

    // =========================================================
    // getUserBookings
    // =========================================================

    @Test
    void getUserBookings_returnsBookings() {
        when(bookingRepository
                .findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(booking()));

        List<BookingResponse> result =
                service.getUserBookings(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());

        verify(bookingRepository)
                .findByUserIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void getUserBookings_returnsEmptyList() {
        when(bookingRepository
                .findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());

        List<BookingResponse> result =
                service.getUserBookings(10L);

        assertTrue(result.isEmpty());
    }

    // =========================================================
    // getBookingsByParkingSpace
    // =========================================================

    @Test
    void getBookingsByParkingSpace_returnsBookings() {
        when(bookingRepository.findByParkingSpaceId(1L))
                .thenReturn(List.of(booking()));

        List<BookingResponse> result =
                service.getBookingsByParkingSpace(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getParkingSpaceId());
    }

    @Test
    void getBookingsByParkingSpace_returnsEmptyList() {
        when(bookingRepository.findByParkingSpaceId(1L))
                .thenReturn(List.of());

        List<BookingResponse> result =
                service.getBookingsByParkingSpace(1L);

        assertTrue(result.isEmpty());
    }

    // =========================================================
    // updateBookingStatus
    // =========================================================

    @Test
    void updateBookingStatus_withRemarks() {
        Booking booking = booking();

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingStatusUpdateRequest request =
                BookingStatusUpdateRequest.builder()
                        .status(BookingStatus.CONFIRMED)
                        .remarks("Payment received")
                        .build();

        BookingResponse result =
                service.updateBookingStatus(1L, request);

        assertEquals(
                BookingStatus.CONFIRMED,
                result.getStatus());

        verify(historyRepository).save(any());
    }

    @Test
    void updateBookingStatus_withoutRemarks() {
        Booking booking = booking();

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingStatusUpdateRequest request =
                BookingStatusUpdateRequest.builder()
                        .status(BookingStatus.CONFIRMED)
                        .remarks(null)
                        .build();

        BookingResponse result =
                service.updateBookingStatus(1L, request);

        assertEquals(
                BookingStatus.CONFIRMED,
                result.getStatus());

        verify(historyRepository).save(any());
    }

    @Test
    void updateBookingStatus_throwsWhenBookingNotFound() {
        when(bookingRepository.findById(99L))
                .thenReturn(Optional.empty());

        BookingStatusUpdateRequest request =
                BookingStatusUpdateRequest.builder()
                        .status(BookingStatus.CONFIRMED)
                        .build();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.updateBookingStatus(
                                99L,
                                request));

        assertEquals(
                "Booking not found with ID: 99",
                exception.getMessage());
    }

    // =========================================================
    // cancelBooking
    // =========================================================

    @Test
    void cancelBooking_success() {
        Booking booking = booking();

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse result =
                service.cancelBooking(10L, 1L);

        assertEquals(
                BookingStatus.CANCELLED,
                result.getStatus());

        verify(bookingRepository)
                .save(booking);

        verify(parkingServiceClient)
                .markUnbooked(100L, secret);

        verify(historyRepository)
                .save(any());
    }

    @Test
    void cancelBooking_throwsWhenBookingNotFound() {
        when(bookingRepository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.cancelBooking(10L, 99L));

        assertEquals(
                "Booking not found with ID: 99",
                exception.getMessage());
    }

    @Test
    void cancelBooking_throwsWhenDifferentUser() {
        Booking booking = booking();

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.cancelBooking(999L, 1L));

        assertEquals(
                "Unauthorized: Booking does not belong to user ID: 999",
                exception.getMessage());

        verify(bookingRepository, never())
                .save(any());
    }

    @Test
    void cancelBooking_throwsWhenAlreadyCompleted() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.cancelBooking(10L, 1L));

        assertEquals(
                "Booking cannot be cancelled in status: COMPLETED",
                exception.getMessage());

        verify(bookingRepository, never())
                .save(any());
    }

    @Test
    void cancelBooking_throwsWhenAlreadyCancelled() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.cancelBooking(10L, 1L));

        assertEquals(
                "Booking cannot be cancelled in status: CANCELLED",
                exception.getMessage());

        verify(bookingRepository, never())
                .save(any());
    }
}