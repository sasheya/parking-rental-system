package com.parking.booking_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import com.parking.booking_service.client.ParkingServiceClient;
import com.parking.booking_service.client.ParkingSpaceResponse;
import com.parking.booking_service.client.AvailabilitySlotResponse;
import com.parking.booking_service.dto.BookingResponse;
import com.parking.booking_service.dto.BookingStatusUpdateRequest;
import com.parking.booking_service.dto.CreateBookingRequest;
import com.parking.booking_service.model.Booking;
import com.parking.booking_service.model.BookingStatus;
import com.parking.booking_service.model.BookingStatusHistory;
import com.parking.booking_service.repository.BookingRepository;
import com.parking.booking_service.repository.BookingStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ParkingServiceClient parkingServiceClient;

    @Value("${internal.service-secret:}")
    private String internalServiceSecret;

    @Override
    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Start time cannot be in the past");
        }

        ParkingSpaceResponse space = null;
        try {
            space = parkingServiceClient.getSpaceById(request.getParkingSpaceId(), internalServiceSecret);
        } catch (Exception e) {
            throw new IllegalStateException("Parking service is unavailable; booking cannot be created", e);
        }

        if (space == null || !Boolean.TRUE.equals(space.getActive())) {
            throw new IllegalArgumentException("Parking space is not available");
        }
        if (space.getPricePerHour() == null || space.getPricePerHour().signum() < 0) {
            throw new IllegalStateException("Parking space has no valid price");
        }
        if (space.getAvailableSlots() == null || space.getAvailableSlots() <= 0) {
            throw new IllegalStateException("Parking space has no available slots");
        }

        AvailabilitySlotResponse slot = parkingServiceClient.getAvailability(request.getParkingSpaceId(), internalServiceSecret).stream()
                .filter(candidate -> request.getSlotId().equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Availability slot does not belong to the parking space"));
        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new IllegalStateException("Availability slot is already booked");
        }
        if (!request.getStartTime().equals(slot.getStartTime()) || !request.getEndTime().equals(slot.getEndTime())) {
            throw new IllegalArgumentException("Booking time must match the selected availability slot");
        }
        log.info("Calling parking-service with secret=[{}]", internalServiceSecret);
        parkingServiceClient.markBooked(request.getSlotId(), internalServiceSecret);

        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(Math.max(1, Math.ceil(minutes / 60.0)));
        BigDecimal totalAmount = space.getPricePerHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);

        Booking booking;
        try {
            booking = bookingRepository.save(Booking.builder()
                    .userId(userId)
                    .parkingSpaceId(request.getParkingSpaceId())
                    .slotId(request.getSlotId())
                    .vehicleId(request.getVehicleId())
                    .startTime(request.getStartTime())
                    .endTime(request.getEndTime())
                    .totalAmount(totalAmount)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .build());
        } catch (RuntimeException exception) {
            parkingServiceClient.markUnbooked(request.getSlotId(), internalServiceSecret);
            throw exception;
        }

        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(booking.getId())
                .status(BookingStatus.PENDING_PAYMENT)
                .remarks("Booking initiated. Awaiting payment.")
                .build());

        log.info("Created booking ID: {} for user ID: {}, totalAmount: {}", booking.getId(), userId, totalAmount);
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByIdForUser(Long userId, Long bookingId) {
        BookingResponse response = getBookingById(bookingId);
        if (!userId.equals(response.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Booking belongs to another renter");
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByParkingSpace(Long parkingSpaceId) {
        return bookingRepository.findByParkingSpaceId(parkingSpaceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatusUpdateRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        booking.setStatus(request.getStatus());
        booking = bookingRepository.save(booking);

        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(booking.getId())
                .status(request.getStatus())
                .remarks(request.getRemarks() != null ? request.getRemarks() : "Status updated to " + request.getStatus())
                .build());

        log.info("Updated booking ID: {} status to {}", bookingId, request.getStatus());
        return mapToResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: Booking does not belong to user ID: " + userId);
        }

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
        parkingServiceClient.markUnbooked(booking.getSlotId(), internalServiceSecret);

        historyRepository.save(BookingStatusHistory.builder()
                .bookingId(booking.getId())
                .status(BookingStatus.CANCELLED)
                .remarks("Cancelled by user ID: " + userId)
                .build());

        log.info("Cancelled booking ID: {} by user ID: {}", bookingId, userId);
        return mapToResponse(booking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .parkingSpaceId(booking.getParkingSpaceId())
                .slotId(booking.getSlotId())
                .vehicleId(booking.getVehicleId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
