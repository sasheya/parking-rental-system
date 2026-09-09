package com.parking.booking_service.service;

import java.util.List;

import com.parking.booking_service.dto.BookingResponse;
import com.parking.booking_service.dto.BookingStatusUpdateRequest;
import com.parking.booking_service.dto.CreateBookingRequest;

public interface BookingService {
    BookingResponse createBooking(Long userId, CreateBookingRequest request);
    BookingResponse getBookingById(Long bookingId);
    BookingResponse getBookingByIdForUser(Long userId, Long bookingId);
    List<BookingResponse> getUserBookings(Long userId);
    List<BookingResponse> getBookingsByParkingSpace(Long parkingSpaceId);
    BookingResponse updateBookingStatus(Long bookingId, BookingStatusUpdateRequest request);
    BookingResponse cancelBooking(Long userId, Long bookingId);
}
