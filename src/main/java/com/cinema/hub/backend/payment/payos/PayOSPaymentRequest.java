package com.cinema.hub.backend.payment.payos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PayOSPaymentRequest {
    long orderCode;
    long amount;
    String description;
    String returnUrl;
    String cancelUrl;
}
