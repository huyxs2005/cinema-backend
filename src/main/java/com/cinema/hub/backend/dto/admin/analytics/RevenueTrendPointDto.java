package com.cinema.hub.backend.dto.admin.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevenueTrendPointDto {

    private final LocalDate date;
    private final BigDecimal paidRevenue;
    private final long paidCount;
    private final long failedCount;
    private final long cancelledCount;
}
