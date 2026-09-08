package com.parking.booking_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.booking_service.dto.BookingResponse;
import com.parking.booking_service.dto.BookingStatusUpdateRequest;
import com.parking.booking_service.dto.CreateBookingRequest;
import com.parking.booking_service.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(Authentication authentication,
                                                         @Valid @RequestBody CreateBookingRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        BookingResponse response = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<BookingResponse> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable("id") Long id) {
        BookingResponse booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/space/{spaceId}")
    public ResponseEntity<List<BookingResponse>> getBookingsBySpace(@PathVariable("spaceId") Long spaceId) {
        List<BookingResponse> bookings = bookingService.getBookingsByParkingSpace(spaceId);
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(@PathVariable("id") Long id,
                                                               @Valid @RequestBody BookingStatusUpdateRequest request) {
        BookingResponse updated = bookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(Authentication authentication,
                                                         @PathVariable("id") Long id) {
        Long userId = getUserIdFromAuth(authentication);
        BookingResponse cancelled = bookingService.cancelBooking(userId, id);
        return ResponseEntity.ok(cancelled);
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        } else if (authentication != null && authentication.getPrincipal() != null) {
            try {
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid authentication principal");
            }
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}
