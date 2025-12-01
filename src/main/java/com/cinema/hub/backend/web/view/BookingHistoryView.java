package com.cinema.hub.backend.web.view;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Builder
public class BookingHistoryView {
    private final String bookingCode;
    private final String movieTitle;
    private final String theaterName;
    private final LocalDateTime showtime;
    private final String seatLabels;
    private final String format;
    private final BookingStatus status;
    private final int ticketCount;
    private final BigDecimal total;
    private final OffsetDateTime purchasedAt;
}
