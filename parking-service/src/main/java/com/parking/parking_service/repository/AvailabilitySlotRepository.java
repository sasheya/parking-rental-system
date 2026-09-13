package com.parking.parking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parking.parking_service.model.AvailabilitySlot;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    List<AvailabilitySlot> findByParkingSpaceId(Long parkingSpaceId);
    List<AvailabilitySlot> findByParkingSpaceIdAndIsBookedFalse(Long parkingSpaceId);
}
