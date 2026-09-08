package com.parking.payment_service.service;

import com.parking.payment_service.dto.PaymentConfirmRequest;
import com.parking.payment_service.dto.PaymentIntentRequest;
import com.parking.payment_service.dto.PaymentIntentResponse;
import com.parking.payment_service.dto.RefundRequest;
import com.parking.payment_service.dto.RefundResponse;

public interface PaymentService {
    PaymentIntentResponse createPaymentIntent(Long userId, PaymentIntentRequest request);
    PaymentIntentResponse confirmPayment(Long userId, PaymentConfirmRequest request);
    PaymentIntentResponse getTransactionByBookingId(Long bookingId);
    RefundResponse processRefund(Long userId, RefundRequest request);
    void handleStripeEvent(String payload, String sigHeader);
}
