package com.parking.payment_service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.parking.payment_service.client.BookingResponse;
import com.parking.payment_service.client.BookingServiceClient;
import com.parking.payment_service.dto.PaymentIntentRequest;
import com.parking.payment_service.repository.RefundRepository;
import com.parking.payment_service.repository.TransactionRepository;
import com.parking.payment_service.service.PaymentServiceImpl;

class PaymentServiceImplTest {

    @Test
    void rejectsPaymentAmountDifferentFromBookingAmount() {
        BookingServiceClient bookingClient = mock(BookingServiceClient.class);
        TransactionRepository transactionRepository = mock(TransactionRepository.class);
        RefundRepository refundRepository = mock(RefundRepository.class);
        BookingResponse booking = new BookingResponse();
        booking.setUserId(7L);
        booking.setTotalAmount(new BigDecimal("12.00"));
        when(bookingClient.getBooking(10L, "secret")).thenReturn(booking);

        PaymentServiceImpl service = new PaymentServiceImpl(transactionRepository, refundRepository, bookingClient);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "internalServiceSecret", "secret");

        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .bookingId(10L)
                .amount(new BigDecimal("10.00"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createPaymentIntent(7L, request));
    }
}
