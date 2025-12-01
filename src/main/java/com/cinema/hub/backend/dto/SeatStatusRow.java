package com.cinema.hub.backend.dto;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.entity.enums.SeatHoldStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SeatStatusRow(
        Integer seatId,
        String rowLabel,
        Integer seatNumber,
        String seatTypeName,
        String coupleGroupId,
        BigDecimal effectivePrice,
        String seatStatus,
        SeatHoldStatus holdStatus,
        OffsetDateTime holdExpiresAt,
        BookingStatus bookingStatus,
        PaymentStatus paymentStatus,
        Integer holdUserId
) {
}
