package com.parking.booking_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;

import com.parking.booking_service.dto.BookingResponse;
import com.parking.booking_service.dto.BookingStatusUpdateRequest;
import com.parking.booking_service.dto.CreateBookingRequest;
import com.parking.booking_service.model.BookingStatus;
import com.parking.booking_service.service.BookingService;
import com.parking.common_security.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @Value("${internal.service-secret:}")
    private String internalServiceSecret;

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'OWNER')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(Authentication authentication,
                                                         @Valid @RequestBody CreateBookingRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        BookingResponse response = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<BookingResponse> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/renter/{renterId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getRenterBookings(@PathVariable Long renterId,
                                                                    Authentication authentication) {
        if (!renterId.equals(getUserIdFromAuth(authentication))) {
            throw new org.springframework.security.access.AccessDeniedException("Bookings belong to another renter");
        }
        return ResponseEntity.ok(ApiResponse.success(bookingService.getUserBookings(renterId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(Authentication authentication,
                                                          @PathVariable("id") Long id) {
        BookingResponse booking = bookingService.getBookingByIdForUser(getUserIdFromAuth(authentication), id);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/internal/{id}")
    public ResponseEntity<BookingResponse> getInternalBooking(@PathVariable Long id,
                                                               @RequestHeader("X-Internal-Secret") String secret) {
        assertInternalSecret(secret);
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/space/{spaceId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsBySpace(@PathVariable("spaceId") Long spaceId) {
        List<BookingResponse> bookings = bookingService.getBookingsByParkingSpace(spaceId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(@PathVariable("id") Long id,
                                                               @Valid @RequestBody BookingStatusUpdateRequest request) {
        BookingResponse updated = bookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<ApiResponse<BookingResponse>> patchStatus(@PathVariable Long id,
                                                       @RequestParam BookingStatus status,
                                                       @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.updateBookingStatus(id,
            BookingStatusUpdateRequest.builder().status(status).remarks(remarks).build())));
    }

    @PutMapping("/internal/{id}/status")
    public ResponseEntity<Void> updateInternalStatus(@PathVariable Long id,
                                                      @RequestHeader("X-Internal-Secret") String secret,
                                                      @Valid @RequestBody BookingStatusUpdateRequest request) {
        assertInternalSecret(secret);
        bookingService.updateBookingStatus(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DRIVER', 'OWNER')")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(Authentication authentication,
                                                         @PathVariable("id") Long id) {
        Long userId = getUserIdFromAuth(authentication);
        BookingResponse cancelled = bookingService.cancelBooking(userId, id);
        return ResponseEntity.ok(ApiResponse.success(cancelled));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DRIVER', 'OWNER')")
    public ResponseEntity<ApiResponse<BookingResponse>> patchCancel(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.cancelBooking(getUserIdFromAuth(authentication), id)));
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

    private void assertInternalSecret(String secret) {
        if (internalServiceSecret.isBlank() || !internalServiceSecret.equals(secret)) {
            throw new org.springframework.security.access.AccessDeniedException("Invalid internal service credentials");
        }
    }
}
