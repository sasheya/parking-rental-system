package com.parking.payment_service.dto;

import java.math.BigDecimal;

import com.parking.payment_service.model.TransactionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponse {
    private Long transactionId;
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private String paymentIntentId;
    private String clientSecret;
    private TransactionStatus status;
}
