package com.cinema.hub.backend.dto.staff.seat;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffSeatHoldRequest {

    private List<Integer> seatIds;
}
