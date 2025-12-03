package com.cinema.hub.backend.dto.admin.analytics;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentMethodBreakdownDto {

    private final String method;
    private final BigDecimal totalAmount;
    private final long count;
    private final BigDecimal percentage;
}
