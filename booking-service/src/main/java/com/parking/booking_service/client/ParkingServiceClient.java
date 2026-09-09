package com.parking.booking_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;

@FeignClient(name = "parking-service")
public interface ParkingServiceClient {

    @GetMapping("/api/parking/internal/{id}")
    ParkingSpaceResponse getSpaceById(@PathVariable("id") Long id,
                                      @RequestHeader("X-Internal-Secret") String secret);

    @GetMapping("/api/parking/internal/{spaceId}/availability")
    List<AvailabilitySlotResponse> getAvailability(@PathVariable("spaceId") Long spaceId,
                                                   @RequestHeader("X-Internal-Secret") String secret);

    @PutMapping("/api/parking/slots/{slotId}/mark-booked")
    void markBooked(@PathVariable("slotId") Long slotId,
                    @RequestHeader("X-Internal-Secret") String secret);

    @PutMapping("/api/parking/slots/{slotId}/mark-unbooked")
    void markUnbooked(@PathVariable("slotId") Long slotId,
                      @RequestHeader("X-Internal-Secret") String secret);
}
