package com.parking.payment_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parking.payment_service.service.PaymentService;
import com.parking.common_security.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> handleStripeWebhook(@RequestBody String payload,
                                                      @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        paymentService.handleStripeEvent(payload, sigHeader);
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("received", true)));
    }
}
