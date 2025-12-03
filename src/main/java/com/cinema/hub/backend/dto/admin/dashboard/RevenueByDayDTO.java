package com.cinema.hub.backend.dto.admin.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class RevenueByDayDTO {

    private LocalDate date;
    private BigDecimal revenue;
    private long successfulOrders;
}
