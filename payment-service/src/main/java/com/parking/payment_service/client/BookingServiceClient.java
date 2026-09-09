package com.parking.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "booking-service")
public interface BookingServiceClient {

    @GetMapping("/api/bookings/internal/{id}")
    BookingResponse getBooking(@PathVariable("id") Long id,
                               @RequestHeader("X-Internal-Secret") String secret);

    @PutMapping("/api/bookings/internal/{id}/status")
    void updateBookingStatus(@PathVariable("id") Long id,
                             @RequestHeader("X-Internal-Secret") String secret,
                             @RequestBody BookingStatusUpdateRequest request);
}
