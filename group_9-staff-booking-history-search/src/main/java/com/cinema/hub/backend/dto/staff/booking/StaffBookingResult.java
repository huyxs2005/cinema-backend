package com.cinema.hub.backend.dto.staff.booking;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingResult {

    private final String bookingCode;
    private final StaffPaymentMethod paymentMethod;
    private final BigDecimal totalAmount;
    private final BigDecimal finalAmount;
}
