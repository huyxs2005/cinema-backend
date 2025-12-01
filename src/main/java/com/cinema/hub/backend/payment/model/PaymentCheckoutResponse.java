package com.cinema.hub.backend.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Value
@Builder
public class PaymentCheckoutResponse {
    boolean success;
    Integer bookingId;
    String orderCode;
    String bookingCode;
    BigDecimal amount;
    String qrBase64;
    String checkoutUrl;
    BankInfo bankInfo;
    String transferContent;
    OffsetDateTime expiresAt;

    @Value
    @Builder
    @AllArgsConstructor
    public static class BankInfo {
        String bank;
        String account;
        String name;
    }
}
