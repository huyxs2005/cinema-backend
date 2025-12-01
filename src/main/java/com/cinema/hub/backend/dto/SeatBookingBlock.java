package com.cinema.hub.backend.dto;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;

public record SeatBookingBlock(
        Integer seatId,
        Integer bookingId,
        Integer bookingUserId,
        BookingStatus bookingStatus,
        PaymentStatus paymentStatus
) {
}
