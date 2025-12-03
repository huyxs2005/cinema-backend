package com.cinema.hub.backend.dto.admin.dashboard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    private BigDecimal revenueToday;
    private BigDecimal revenueYesterday;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueThisYear;
    private BigDecimal revenueLastMonth;
    private BigDecimal revenueLastYear;
    private BigDecimal revenueCustom;
    private OffsetDateTime customStart;
    private OffsetDateTime customEnd;
    private BigDecimal selectedRevenue;
    private OffsetDateTime selectedRangeStart;
    private OffsetDateTime selectedRangeEnd;
    private long totalOrders;
    private long completedOrders;
    private long pendingOrders;
    private long failedOrders;
    private long seatsSold;
    private BigDecimal paymentSuccessRate;
    private OffsetDateTime generatedAt;
}
