package com.parking.payment_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.parking.payment_service.client.BookingResponse;
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
import com.parking.payment_service.service.PaymentServiceImpl;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private BookingServiceClient bookingServiceClient;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                transactionRepository,
                refundRepository,
                bookingServiceClient
        );

        ReflectionTestUtils.setField(
                service,
                "internalServiceSecret",
                "secret"
        );

        // Use mock/simulation mode instead of calling Stripe.
        ReflectionTestUtils.setField(
                service,
                "stripeSecretKey",
                null
        );

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookSecret",
                null
        );

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookDemoMode",
                false
        );
    }


    // createPaymentIntent()
    @Test
    void createPaymentIntent_success_mockStripe() {

        BookingResponse booking = booking(10L, 7L, "12.00");

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(booking);

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.empty());

        when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("12.00"))
                .currency("INR")
                .build();

        PaymentIntentResponse response =
                service.createPaymentIntent(7L, request);

        assertNotNull(response);
        assertEquals(1L, response.getTransactionId());
        assertEquals(10L, response.getBookingId());
        assertEquals(new BigDecimal("12.00"), response.getAmount());
        assertEquals("INR", response.getCurrency());
        assertTrue(response.getPaymentIntentId().startsWith("pi_mock_"));
        assertTrue(response.getClientSecret().endsWith("_secret_mock"));
        assertEquals(TransactionStatus.PENDING, response.getStatus());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createPaymentIntent_success_defaultCurrencyWhenNull() {

        BookingResponse booking = booking(10L, 7L, "12.00");

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(booking);

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.empty());

        Transaction transaction = Transaction.builder()
                .id(1L)
                .bookingId(10L)
                .userId(7L)
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("12.00"))
                .currency(null)
                .build();

        PaymentIntentResponse response =
                service.createPaymentIntent(7L, request);

        assertEquals("INR", response.getCurrency());
    }

    @Test
    void createPaymentIntent_updatesExistingTransaction() {

        BookingResponse booking = booking(10L, 7L, "12.00");

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(booking);

        Transaction existing = Transaction.builder()
                .id(20L)
                .bookingId(10L)
                .userId(7L)
                .build();

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.of(existing));

        when(transactionRepository.save(existing))
                .thenReturn(existing);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("12.00"))
                .currency("usd")
                .build();

        PaymentIntentResponse response =
                service.createPaymentIntent(7L, request);

        assertEquals(20L, response.getTransactionId());
        assertEquals("USD", response.getCurrency());
        assertEquals(TransactionStatus.PENDING, existing.getStatus());

        verify(transactionRepository).save(existing);
    }

    @Test
    void createPaymentIntent_rejectsNullBooking() {

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(null);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("10.00"))
                .build();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createPaymentIntent(7L, request)
                );

        assertEquals(
                "Booking does not belong to the authenticated user",
                exception.getMessage()
        );
    }

    @Test
    void createPaymentIntent_rejectsBookingBelongingToAnotherUser() {

        BookingResponse booking = booking(10L, 99L, "10.00");

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(booking);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("10.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPaymentIntent(7L, request)
        );
    }

    @Test
    void createPaymentIntent_rejectsAmountMismatch() {

        BookingResponse booking = booking(10L, 7L, "12.00");

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(booking);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("10.00"))
                .build();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.createPaymentIntent(7L, request)
                );

        assertEquals(
                "Payment amount does not match the booking amount",
                exception.getMessage()
        );
    }

    @Test
    void createPaymentIntent_rejectsNullBookingAmount() {

        BookingResponse booking = booking(10L, 7L, null);

        when(bookingServiceClient.getBooking(10L, "secret"))
                .thenReturn(booking);

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("10.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createPaymentIntent(7L, request)
        );
    }

    // ============================================================
    // confirmPayment()
    // ============================================================

    @Test
    void confirmPayment_success_withoutPaymentMethod() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "12.00",
                TransactionStatus.PENDING
        );

        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findByPaymentIntentId("pi_mock_123"))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentIntentId("pi_mock_123")
                .paymentMethod(null)
                .build();

        PaymentIntentResponse response =
                service.confirmPayment(7L, request);

        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals("pi_mock_123", response.getPaymentIntentId());

        verify(transactionRepository).save(transaction);
        verify(bookingServiceClient).updateBookingStatus(
                eq(10L),
                eq("secret"),
                any(BookingStatusUpdateRequest.class)
        );
    }

    @Test
    void confirmPayment_success_withPaymentMethod() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "12.00",
                TransactionStatus.PENDING
        );

        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findByPaymentIntentId("pi_mock_123"))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentIntentId("pi_mock_123")
                .paymentMethod("CARD")
                .build();

        service.confirmPayment(7L, request);

        assertEquals("CARD", transaction.getPaymentMethod());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
    }

    @Test
    void confirmPayment_rejectsMissingTransaction() {

        when(transactionRepository.findByPaymentIntentId("pi_missing"))
                .thenReturn(Optional.empty());

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentIntentId("pi_missing")
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmPayment(7L, request)
        );
    }

    @Test
    void confirmPayment_rejectsWrongUser() {

        Transaction transaction = transaction(
                1L,
                10L,
                99L,
                "12.00",
                TransactionStatus.PENDING
        );

        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findByPaymentIntentId("pi_mock_123"))
                .thenReturn(Optional.of(transaction));

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentIntentId("pi_mock_123")
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.confirmPayment(7L, request)
        );
    }

    @Test
    void confirmPayment_bookingUpdateFailureThrowsIllegalStateException() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "12.00",
                TransactionStatus.PENDING
        );

        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findByPaymentIntentId("pi_mock_123"))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        doThrow(new RuntimeException("booking service unavailable"))
                .when(bookingServiceClient)
                .updateBookingStatus(
                        anyLong(),
                        anyString(),
                        any(BookingStatusUpdateRequest.class)
                );

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentIntentId("pi_mock_123")
                .build();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.confirmPayment(7L, request)
                );

        assertEquals(
                "Payment succeeded but booking confirmation failed",
                exception.getMessage()
        );
    }

    // ============================================================
    // Transaction lookup
    // ============================================================

    @Test
    void getTransactionByBookingId_success() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "20.00",
                TransactionStatus.SUCCESS
        );

        transaction.setCurrency("INR");
        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.of(transaction));

        PaymentIntentResponse response =
                service.getTransactionByBookingId(10L);

        assertEquals(1L, response.getTransactionId());
        assertEquals(10L, response.getBookingId());
        assertEquals(new BigDecimal("20.00"), response.getAmount());
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
    }

    @Test
    void getTransactionByBookingId_notFound() {

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getTransactionByBookingId(10L)
        );
    }

    @Test
    void getTransactionById_success() {

        Transaction transaction = transaction(
                5L,
                10L,
                7L,
                "20.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findById(5L))
                .thenReturn(Optional.of(transaction));

        PaymentIntentResponse response =
                service.getTransactionById(5L);

        assertEquals(5L, response.getTransactionId());
    }

    @Test
    void getTransactionById_notFound() {

        when(transactionRepository.findById(5L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.getTransactionById(5L)
        );
    }

    @Test
    void getTransactionByBookingIdForUser_success() {

        Transaction transaction = transaction(
                5L,
                10L,
                7L,
                "20.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.findById(5L))
                .thenReturn(Optional.of(transaction));

        PaymentIntentResponse response =
                service.getTransactionByBookingIdForUser(7L, 10L);

        assertEquals(5L, response.getTransactionId());
    }

    @Test
    void getTransactionByBookingIdForUser_wrongUser() {

        Transaction transaction = transaction(
                5L,
                10L,
                99L,
                "20.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findByBookingId(10L))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.findById(5L))
                .thenReturn(Optional.of(transaction));

        assertThrows(
                AccessDeniedException.class,
                () -> service.getTransactionByBookingIdForUser(7L, 10L)
        );
    }

    @Test
    void getTransactionByIdForUser_success() {

        Transaction transaction = transaction(
                5L,
                10L,
                7L,
                "20.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findById(5L))
                .thenReturn(Optional.of(transaction));

        PaymentIntentResponse response =
                service.getTransactionByIdForUser(7L, 5L);

        assertEquals(5L, response.getTransactionId());
    }

    @Test
    void getTransactionByIdForUser_wrongUser() {

        Transaction transaction = transaction(
                5L,
                10L,
                99L,
                "20.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findById(5L))
                .thenReturn(Optional.of(transaction));

        assertThrows(
                AccessDeniedException.class,
                () -> service.getTransactionByIdForUser(7L, 5L)
        );
    }

    // Refunds
    @Test
    void processRefund_success_mockStripe() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "100.00",
                TransactionStatus.SUCCESS
        );

        transaction.setPaymentIntentId("pi_mock_123");

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("40.00"))
                .reason("Customer requested refund")
                .build();

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(refundRepository.findByTransactionId(1L))
                .thenReturn(List.of());

        Refund savedRefund = Refund.builder()
                .id(50L)
                .transactionId(1L)
                .amount(new BigDecimal("40.00"))
                .reason("Customer requested refund")
                .stripeRefundId("re_mock_123")
                .createdAt(LocalDateTime.now())
                .build();

        when(refundRepository.save(any(Refund.class)))
                .thenReturn(savedRefund);

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        RefundResponse response =
                service.processRefund(7L, request);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(1L, response.getTransactionId());
        assertEquals(
                new BigDecimal("40.00"),
                response.getAmount()
        );
        assertEquals(
                "Customer requested refund",
                response.getReason()
        );

        assertEquals(
                TransactionStatus.REFUNDED,
                transaction.getStatus()
        );

        verify(refundRepository).save(any(Refund.class));
        verify(transactionRepository).save(transaction);
        verify(bookingServiceClient).updateBookingStatus(
                eq(10L),
                eq("secret"),
                any(BookingStatusUpdateRequest.class)
        );
    }

    @Test
    void processRefund_transactionNotFound() {

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.empty());

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("20.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.processRefund(7L, request)
        );
    }

    @Test
    void processRefund_rejectsNonSuccessfulTransaction() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "100.00",
                TransactionStatus.PENDING
        );

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("20.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.processRefund(7L, request)
        );
    }

    @Test
    void processRefund_rejectsWrongUser() {

        Transaction transaction = transaction(
                1L,
                10L,
                99L,
                "100.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("20.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.processRefund(7L, request)
        );
    }

    @Test
    void processRefund_rejectsAmountGreaterThanTransaction() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "100.00",
                TransactionStatus.SUCCESS
        );

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("150.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.processRefund(7L, request)
        );
    }

    @Test
    void processRefund_rejectsCumulativeRefundOverTransactionAmount() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "100.00",
                TransactionStatus.SUCCESS
        );

        Refund previousRefund = Refund.builder()
                .id(10L)
                .transactionId(1L)
                .amount(new BigDecimal("80.00"))
                .build();

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(refundRepository.findByTransactionId(1L))
                .thenReturn(List.of(previousRefund));

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("30.00"))
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.processRefund(7L, request)
        );
    }

    @Test
    void processRefund_bookingServiceFailureDoesNotFailRefund() {

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "100.00",
                TransactionStatus.SUCCESS
        );

        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findById(1L))
                .thenReturn(Optional.of(transaction));

        when(refundRepository.findByTransactionId(1L))
                .thenReturn(List.of());

        Refund refund = Refund.builder()
                .id(50L)
                .transactionId(1L)
                .amount(new BigDecimal("20.00"))
                .stripeRefundId("re_mock_123")
                .createdAt(LocalDateTime.now())
                .build();

        when(refundRepository.save(any(Refund.class)))
                .thenReturn(refund);

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        doThrow(new RuntimeException("Booking service unavailable"))
                .when(bookingServiceClient)
                .updateBookingStatus(
                        anyLong(),
                        anyString(),
                        any(BookingStatusUpdateRequest.class)
                );

        RefundRequest request = RefundRequest.builder()
                .transactionId(1L)
                .amount(new BigDecimal("20.00"))
                .build();

        RefundResponse response =
                service.processRefund(7L, request);

        assertNotNull(response);
        assertEquals(
                TransactionStatus.REFUNDED,
                transaction.getStatus()
        );
    }

    // Stripe webhook - demo mode
    @Test
    void handleStripeEvent_demoMode_successfulPayment() {

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookDemoMode",
                true
        );

        Transaction transaction = transaction(
                1L,
                10L,
                7L,
                "100.00",
                TransactionStatus.PENDING
        );

        transaction.setPaymentIntentId("pi_mock_123");

        when(transactionRepository.findByPaymentIntentId("pi_mock_123"))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(transaction))
                .thenReturn(transaction);

        String payload = """
                {
                    "type": "payment_intent.succeeded",
                    "data": {
                        "object": {
                            "id": "pi_mock_123"
                        }
                    }
                }
                """;

        service.handleStripeEvent(payload, null);

        assertEquals(
                TransactionStatus.SUCCESS,
                transaction.getStatus()
        );

        verify(transactionRepository).save(transaction);
    }

    @Test
    void handleStripeEvent_demoMode_nonPaymentEvent() {

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookDemoMode",
                true
        );

        String payload = """
                {
                    "type": "payment_intent.created"
                }
                """;

        assertDoesNotThrow(
                () -> service.handleStripeEvent(payload, null)
        );

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void handleStripeEvent_demoMode_invalidPayload() {

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookDemoMode",
                true
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.handleStripeEvent("invalid-json", null)
        );
    }

    @Test
    void handleStripeEvent_withoutWebhookSecret_rejectsRequest() {

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookDemoMode",
                false
        );

        ReflectionTestUtils.setField(
                service,
                "stripeWebhookSecret",
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.handleStripeEvent("payload", "signature")
        );
    }

    // ============================================================
    // Stripe initialization
    // ============================================================

    @Test
    void initStripe_withoutSecret_doesNotFail() {

        ReflectionTestUtils.setField(
                service,
                "stripeSecretKey",
                null
        );

        assertDoesNotThrow(
                () -> service.initStripe()
        );
    }

    // ============================================================
    // Helper methods
    // ============================================================

    private BookingResponse booking(
            Long id,
            Long userId,
            String amount) {

        BookingResponse booking = new BookingResponse();

        booking.setId(id);
        booking.setUserId(userId);

        if (amount != null) {
            booking.setTotalAmount(
                    new BigDecimal(amount)
            );
        }

        return booking;
    }

    private Transaction transaction(
            Long id,
            Long bookingId,
            Long userId,
            String amount,
            TransactionStatus status) {

        return Transaction.builder()
                .id(id)
                .bookingId(bookingId)
                .userId(userId)
                .amount(new BigDecimal(amount))
                .currency("INR")
                .paymentIntentId("pi_mock_123")
                .clientSecret("client_secret")
                .status(status)
                .build();
    }
}