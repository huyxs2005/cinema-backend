package com.cinema.hub.backend.dto.staff.booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingStatusResponse {

    private final String bookingStatus;
    private final String paymentStatus;
}
