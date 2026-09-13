package com.parking.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private Long id;
    private Long transactionId;
    private BigDecimal amount;
    private String reason;
    private String stripeRefundId;
    private LocalDateTime createdAt;
}
