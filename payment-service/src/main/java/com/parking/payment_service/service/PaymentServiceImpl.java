package com.parking.payment_service.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.payment_service.client.BookingServiceClient;
import com.parking.payment_service.client.BookingStatusUpdateRequest;
import com.parking.payment_service.dto.PaymentConfirmRequest;
import com.parking.payment_service.dto.PaymentIntentRequest;
import com.parking.payment_service.dto.PaymentIntentResponse;
import com.parking.payment_service.dto.RefundRequest;
import com.parking.payment_service.dto.RefundResponse;
import com.parking.payment_service.model.Refund;
import com.parking.payment_service.model.Transaction;
import com.parking.payment_service.model.TransactionStatus;
import com.parking.payment_service.repository.RefundRepository;
import com.parking.payment_service.repository.TransactionRepository;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final RefundRepository refundRepository;
    private final BookingServiceClient bookingServiceClient;

    @Value("${stripe.secret-key:#{null}}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:#{null}}")
    private String stripeWebhookSecret;

    @PostConstruct
    public void initStripe() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
            log.info("Initialized Stripe API client with configured secret key.");
        } else {
            log.info("Stripe secret key not provided. Operating in test/simulation mode.");
        }
    }

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(Long userId, PaymentIntentRequest request) {
        String currency = request.getCurrency() != null ? request.getCurrency().toLowerCase() : "usd";
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String paymentIntentId;
        String clientSecret;

        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            try {
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency(currency)
                        .putMetadata("bookingId", request.getBookingId().toString())
                        .putMetadata("userId", userId.toString())
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                        )
                        .build();

                PaymentIntent intent = PaymentIntent.create(params);
                paymentIntentId = intent.getId();
                clientSecret = intent.getClientSecret();
            } catch (Exception e) {
                log.warn("Stripe API call failed, generating simulated payment intent", e);
                paymentIntentId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                clientSecret = paymentIntentId + "_secret_mock";
            }
        } else {
            paymentIntentId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            clientSecret = paymentIntentId + "_secret_mock";
        }

        Transaction transaction = transactionRepository.findByBookingId(request.getBookingId())
                .orElseGet(() -> Transaction.builder()
                        .bookingId(request.getBookingId())
                        .userId(userId)
                        .build());

        transaction.setAmount(request.getAmount());
        transaction.setCurrency(currency.toUpperCase());
        transaction.setPaymentIntentId(paymentIntentId);
        transaction.setClientSecret(clientSecret);
        transaction.setStatus(TransactionStatus.PENDING);

        transaction = transactionRepository.save(transaction);
        log.info("Created transaction ID: {} for booking ID: {}", transaction.getId(), request.getBookingId());

        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public PaymentIntentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        Transaction transaction = transactionRepository.findByPaymentIntentId(request.getPaymentIntentId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for payment intent ID: " + request.getPaymentIntentId()));

        transaction.setStatus(TransactionStatus.SUCCESS);
        if (request.getPaymentMethod() != null) {
            transaction.setPaymentMethod(request.getPaymentMethod());
        }
        transaction = transactionRepository.save(transaction);

        try {
            bookingServiceClient.updateBookingStatus(
                    transaction.getBookingId(),
                    BookingStatusUpdateRequest.builder()
                            .status("CONFIRMED")
                            .remarks("Payment confirmed via transaction ID: " + transaction.getId())
                            .build()
            );
            log.info("Successfully notified Booking Service to confirm booking ID: {}", transaction.getBookingId());
        } catch (Exception e) {
            log.warn("Failed to notify Booking Service via Feign during payment confirmation", e);
        }

        return mapToResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntentResponse getTransactionByBookingId(Long bookingId) {
        Transaction transaction = transactionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for booking ID: " + bookingId));
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public RefundResponse processRefund(Long userId, RefundRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + request.getTransactionId()));

        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new IllegalArgumentException("Cannot refund transaction in status: " + transaction.getStatus());
        }

        String stripeRefundId = "re_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Refund refund = Refund.builder()
                .transactionId(transaction.getId())
                .amount(request.getAmount())
                .reason(request.getReason())
                .stripeRefundId(stripeRefundId)
                .build();

        refund = refundRepository.save(refund);

        transaction.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(transaction);

        try {
            bookingServiceClient.updateBookingStatus(
                    transaction.getBookingId(),
                    BookingStatusUpdateRequest.builder()
                            .status("CANCELLED")
                            .remarks("Booking refunded via refund ID: " + refund.getId())
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to notify Booking Service during refund", e);
        }

        log.info("Processed refund ID: {} for transaction ID: {}", refund.getId(), transaction.getId());
        return mapToRefundResponse(refund);
    }

    @Override
    @Transactional
    public void handleStripeEvent(String payload, String sigHeader) {
        try {
            Event event;
            if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()) {
                event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            } else {
                log.info("Stripe webhook secret not configured. Skipping signature verification.");
                return;
            }

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    confirmPaymentInternal(intent.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook event", e);
        }
    }

    private void confirmPaymentInternal(String paymentIntentId) {
        transactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);
            try {
                bookingServiceClient.updateBookingStatus(
                        tx.getBookingId(),
                        BookingStatusUpdateRequest.builder()
                                .status("CONFIRMED")
                                .remarks("Payment confirmed via webhook for payment intent: " + paymentIntentId)
                                .build()
                );
            } catch (Exception e) {
                log.warn("Failed to update booking status via Feign on webhook event", e);
            }
        });
    }

    private PaymentIntentResponse mapToResponse(Transaction transaction) {
        return PaymentIntentResponse.builder()
                .transactionId(transaction.getId())
                .bookingId(transaction.getBookingId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentIntentId(transaction.getPaymentIntentId())
                .clientSecret(transaction.getClientSecret())
                .status(transaction.getStatus())
                .build();
    }

    private RefundResponse mapToRefundResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .transactionId(refund.getTransactionId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .stripeRefundId(refund.getStripeRefundId())
                .createdAt(refund.getCreatedAt())
                .build();
    }
}
