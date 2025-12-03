package com.cinema.hub.backend.dto.admin.analytics;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusBreakdownDto {

    private final String bookingStatus;
    private final String paymentStatus;
    private final long count;
    private final BigDecimal revenue;
    private final BigDecimal percentage;
}
