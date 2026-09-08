package com.parking.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parking.payment_service.model.Refund;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByTransactionId(Long transactionId);
}
