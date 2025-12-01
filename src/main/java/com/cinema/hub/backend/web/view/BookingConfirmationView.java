package com.cinema.hub.backend.web.view;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BookingConfirmationView {
    private final String bookingCode;
    private final String movieTitle;
    private final String theaterName;
    private final String auditoriumName;
    private final String format;
    private final String email;
    private final List<String> seatLabels;
    private final String showtimeText;
    private final BigDecimal total;

    public String getSeatLabelsAsString() {
        return String.join(", ", seatLabels);
    }

    public String getQrCodeUrl() {
        return "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=" + bookingCode;
    }
}
