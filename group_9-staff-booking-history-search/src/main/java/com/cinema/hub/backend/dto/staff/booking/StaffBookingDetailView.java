package com.cinema.hub.backend.dto.staff.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingDetailView {

    private final String bookingCode;
    private final String movieTitle;
    private final String auditoriumName;
    private final LocalDateTime showtimeStart;
    private final List<StaffBookingSeatDto> seats;
    private final List<StaffBookingComboDto> combos;
    private final String bookingStatus;
    private final String paymentStatus;
    private final String paymentMethod;
    private final BigDecimal totalAmount;
    private final BigDecimal finalAmount;
    private final String customerPhone;
    private final String customerEmail;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime paidAt;
    private final OffsetDateTime cancelledAt;
    private final String staffName;
    private final boolean canMarkPaid;
    private final boolean canCancel;
    private final boolean showQrButton;
}
