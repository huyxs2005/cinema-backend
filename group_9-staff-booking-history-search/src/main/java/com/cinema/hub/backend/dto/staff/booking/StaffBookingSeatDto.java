package com.cinema.hub.backend.dto.staff.booking;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffBookingSeatDto {

    private final String rowLabel;
    private final Integer seatNumber;
    private final BigDecimal price;
}
