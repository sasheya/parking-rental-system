package com.parking.booking_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "parking-service")
public interface ParkingServiceClient {

    @GetMapping("/api/parking/{id}")
    ParkingSpaceResponse getSpaceById(@PathVariable("id") Long id);
}
