package com.parking.booking_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parking.booking_service.model.BookingStatusHistory;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
