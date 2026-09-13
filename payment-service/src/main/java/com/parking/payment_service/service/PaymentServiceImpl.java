package com.parking.payment_service.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.payment_service.client.BookingServiceClient;
import com.parking.payment_service.client.BookingResponse;
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
import com.stripe.param.RefundCreateParams;

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

    @Value("${internal.service-secret:}")
    private String internalServiceSecret;

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
        BookingResponse booking = bookingServiceClient.getBooking(request.getBookingId(), internalServiceSecret);
        if (booking == null || !userId.equals(booking.getUserId())) {
            throw new IllegalArgumentException("Booking does not belong to the authenticated user");
        }
        if (booking.getTotalAmount() == null || booking.getTotalAmount().compareTo(request.getAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match the booking amount");
        }

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
                throw new IllegalStateException("Could not create payment intent with Stripe", e);
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

        if (!userId.equals(transaction.getUserId())) {
            throw new IllegalArgumentException("Transaction does not belong to the authenticated user");
        }

        if (stripeSecretKey != null && !stripeSecretKey.isBlank()
                && request.getPaymentIntentId() != null
                && !request.getPaymentIntentId().startsWith("pi_mock_")) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());
                if (!"succeeded".equals(intent.getStatus())) {
                    throw new IllegalArgumentException("Payment intent has not succeeded");
                }
            } catch (IllegalArgumentException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("Could not verify payment with Stripe", exception);
            }
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        if (request.getPaymentMethod() != null) {
            transaction.setPaymentMethod(request.getPaymentMethod());
        }
        transaction = transactionRepository.save(transaction);

        try {
            bookingServiceClient.updateBookingStatus(
                    transaction.getBookingId(),
                    internalServiceSecret,
                    BookingStatusUpdateRequest.builder()
                            .status("CONFIRMED")
                            .remarks("Payment confirmed via transaction ID: " + transaction.getId())
                            .build()
            );
            log.info("Successfully notified Booking Service to confirm booking ID: {}", transaction.getBookingId());
        } catch (Exception e) {
            throw new IllegalStateException("Payment succeeded but booking confirmation failed", e);
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
    @Transactional(readOnly = true)
    public PaymentIntentResponse getTransactionById(Long transactionId) {
        return mapToResponse(transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntentResponse getTransactionByBookingIdForUser(Long userId, Long bookingId) {
        PaymentIntentResponse response = getTransactionByBookingId(bookingId);
        assertOwner(userId, response.getTransactionId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentIntentResponse getTransactionByIdForUser(Long userId, Long transactionId) {
        PaymentIntentResponse response = getTransactionById(transactionId);
        assertOwner(userId, transactionId);
        return response;
    }

    private void assertOwner(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        if (!userId.equals(transaction.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Transaction belongs to another user");
        }
    }

    @Override
    @Transactional
    public RefundResponse processRefund(Long userId, RefundRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + request.getTransactionId()));

        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new IllegalArgumentException("Cannot refund transaction in status: " + transaction.getStatus());
        }
        if (!userId.equals(transaction.getUserId())) {
            throw new IllegalArgumentException("Transaction does not belong to the authenticated user");
        }
        if (request.getAmount().compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed the transaction amount");
        }

        BigDecimal alreadyRefunded = refundRepository.findByTransactionId(transaction.getId()).stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alreadyRefunded.add(request.getAmount()).compareTo(transaction.getAmount()) > 0) {
            throw new IllegalArgumentException("Cumulative refunds cannot exceed the transaction amount");
        }

        String stripeRefundId;
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()
                && transaction.getPaymentIntentId() != null
                && !transaction.getPaymentIntentId().startsWith("pi_mock_")) {
            try {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(transaction.getPaymentIntentId())
                        .setAmount(request.getAmount().movePointRight(2).longValueExact())
                        .build();
                stripeRefundId = com.stripe.model.Refund.create(params).getId();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not create Stripe refund", exception);
            }
        } else {
            stripeRefundId = "re_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
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
                    internalServiceSecret,
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
            if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
                throw new IllegalStateException("Stripe webhook secret is not configured");
            }
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    confirmPaymentInternal(intent.getId());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Stripe webhook", e);
        }
    }

    private void confirmPaymentInternal(String paymentIntentId) {
        transactionRepository.findByPaymentIntentId(paymentIntentId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);
            try {
                bookingServiceClient.updateBookingStatus(
                        tx.getBookingId(),
                    internalServiceSecret,
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
