package com.cinema.hub.backend.payment.payos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentData {
    private String bin;
    private String accountNumber;
    private String accountName;
    private BigDecimal amount;
    private String description;
    private long orderCode;
    private String currency;
    private String paymentLinkId;
    private String status;
    private OffsetDateTime expiredAt;
    private String checkoutUrl;
    private String qrCode;
}
