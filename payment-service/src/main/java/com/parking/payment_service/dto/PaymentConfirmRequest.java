package com.parking.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmRequest {

    @NotBlank(message = "Payment intent ID is required")
    private String paymentIntentId;

    private String paymentMethod;
}
