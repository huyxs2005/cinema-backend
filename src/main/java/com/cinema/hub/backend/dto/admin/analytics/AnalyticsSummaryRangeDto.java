package com.cinema.hub.backend.dto.admin.analytics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyticsSummaryRangeDto {

    private final BigDecimal totalRevenue;
    private final long totalOrders;
    private final long totalPaidOrders;
    private final long totalFailedOrders;
    private final long totalCancelledOrders;
    private final Map<String, BigDecimal> revenueByPaymentMethod;
    private final BigDecimal previousPeriodRevenue;
    private final BigDecimal revenueGrowthPercent;
    private final OffsetDateTime createdFrom;
    private final OffsetDateTime createdTo;
    private final OffsetDateTime earliestRecord;
    private final OffsetDateTime latestRecord;
}
