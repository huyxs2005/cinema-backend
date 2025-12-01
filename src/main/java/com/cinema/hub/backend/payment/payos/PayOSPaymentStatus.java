package com.cinema.hub.backend.payment.payos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentStatus {
    private long orderCode;
    private BigDecimal amount;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private String status;
}
