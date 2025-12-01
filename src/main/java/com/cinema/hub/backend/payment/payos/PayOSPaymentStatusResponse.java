package com.cinema.hub.backend.payment.payos;

import lombok.Data;

@Data
public class PayOSPaymentStatusResponse {
    private String code;
    private String desc;
    private PayOSPaymentStatus data;
}
