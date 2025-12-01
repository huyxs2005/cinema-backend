package com.cinema.hub.backend.payment.service;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.payment.model.PaymentStatusResponse;
import com.cinema.hub.backend.payment.util.PaymentException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getPaymentStatus() == null) {
            throw new PaymentException("Booking missing payment status");
        }
        return new PaymentStatusResponse(bookingId, booking.getPaymentStatus());
    }
}
