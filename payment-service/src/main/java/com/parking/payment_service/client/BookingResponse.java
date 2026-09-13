package com.parking.payment_service.client;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private Long userId;
    private BigDecimal totalAmount;
}