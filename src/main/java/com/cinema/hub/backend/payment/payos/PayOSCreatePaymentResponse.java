package com.cinema.hub.backend.payment.payos;

import lombok.Data;

@Data
public class PayOSCreatePaymentResponse {
    private String code;
    private String desc;
    private PayOSPaymentData data;
}
