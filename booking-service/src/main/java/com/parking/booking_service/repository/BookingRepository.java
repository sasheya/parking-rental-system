package com.parking.booking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parking.booking_service.model.Booking;
import com.parking.booking_service.model.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByParkingSpaceId(Long parkingSpaceId);
    List<Booking> findByStatus(BookingStatus status);
}
