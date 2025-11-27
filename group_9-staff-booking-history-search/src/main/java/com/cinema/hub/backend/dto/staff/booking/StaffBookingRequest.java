package com.cinema.hub.backend.dto.staff.booking;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingRequest {

    private final Integer showtimeId;
    private final List<Integer> seatIds;
    private final List<StaffComboSelection> comboSelections;
    private final String customerEmail;
    private final String customerPhone;
    private final StaffPaymentMethod paymentMethod;
}
