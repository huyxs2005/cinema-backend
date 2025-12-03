package com.cinema.hub.backend.dto.admin.dashboard;

import java.math.BigDecimal;
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
public class PaymentChannelDTO {

    private String channel;
    private long totalOrders;
    private BigDecimal totalAmount;
    private BigDecimal ratio;
}
