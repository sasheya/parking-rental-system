package com.parking.payment_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.payment_service.dto.PaymentConfirmRequest;
import com.parking.payment_service.dto.PaymentIntentRequest;
import com.parking.payment_service.dto.PaymentIntentResponse;
import com.parking.payment_service.dto.RefundRequest;
import com.parking.payment_service.dto.RefundResponse;
import com.parking.payment_service.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(Authentication authentication,
                                                                      @Valid @RequestBody PaymentIntentRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentIntentResponse> confirmPayment(Authentication authentication,
                                                                @Valid @RequestBody PaymentConfirmRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        PaymentIntentResponse response = paymentService.confirmPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentIntentResponse> getTransactionByBookingId(@PathVariable("bookingId") Long bookingId) {
        PaymentIntentResponse response = paymentService.getTransactionByBookingId(bookingId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> processRefund(Authentication authentication,
                                                        @Valid @RequestBody RefundRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        RefundResponse response = paymentService.processRefund(userId, request);
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        } else if (authentication != null && authentication.getPrincipal() != null) {
            try {
                return Long.parseLong(authentication.getPrincipal().toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid authentication principal");
            }
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}
