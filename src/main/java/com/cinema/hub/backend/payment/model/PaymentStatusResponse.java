package com.cinema.hub.backend.payment.model;

import com.cinema.hub.backend.entity.enums.PaymentStatus;
import lombok.Value;

@Value
public class PaymentStatusResponse {
    Integer bookingId;
    PaymentStatus status;
}
