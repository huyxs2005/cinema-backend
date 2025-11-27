package com.cinema.hub.backend.dto.staff.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingSummaryDto {

    private final String bookingCode;
    private final String movieTitle;
    private final String auditoriumName;
    private final LocalDateTime showtimeStart;
    private final String paymentMethod;
    private final String bookingStatus;
    private final String paymentStatus;
    private final BigDecimal finalAmount;
    private final String customerPhone;
    private final OffsetDateTime createdAt;
    private final String staffName;
}
