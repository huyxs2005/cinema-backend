package com.cinema.hub.backend.dto;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CancelBookingResponse {
    private Integer bookingId;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private boolean refundTriggered;
}
