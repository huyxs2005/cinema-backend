package com.cinema.hub.backend.dto.staff.seat;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffSeatMapView {

    private final Integer showtimeId;
    private final String movieTitle;
    private final String auditoriumName;
    private final LocalDateTime startTime;
    private final int totalSeats;
    private final int soldSeats;
    private final int heldSeats;
    private final List<StaffSeatDto> seats;
}
