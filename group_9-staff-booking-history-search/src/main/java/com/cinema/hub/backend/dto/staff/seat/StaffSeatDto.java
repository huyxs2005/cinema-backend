package com.cinema.hub.backend.dto.staff.seat;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffSeatDto {

    private final Integer showtimeSeatId;
    private final String rowLabel;
    private final Integer seatNumber;
    private final String seatType;
    private final BigDecimal price;
    private final String status;
    private final boolean couple;
    private final String couplePairId;
    private final boolean heldByCurrentUser;
}
