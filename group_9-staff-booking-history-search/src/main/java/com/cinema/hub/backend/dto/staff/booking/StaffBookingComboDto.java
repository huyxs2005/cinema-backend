package com.cinema.hub.backend.dto.staff.booking;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingComboDto {

    private final String name;
    private final Integer quantity;
    private final BigDecimal totalPrice;
}
