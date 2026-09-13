package com.parking.payment_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parking.payment_service.model.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByBookingId(Long bookingId);
    Optional<Transaction> findByPaymentIntentId(String paymentIntentId);
    List<Transaction> findByUserId(Long userId);
}
