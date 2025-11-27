package com.cinema.hub.backend.dto.staff.seat;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffSeatHoldResponse {

    private final List<Integer> seatIds;
    private final String status;
}
