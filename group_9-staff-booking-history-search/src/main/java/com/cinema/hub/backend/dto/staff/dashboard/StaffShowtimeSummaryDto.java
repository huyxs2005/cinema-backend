package com.cinema.hub.backend.dto.staff.dashboard;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffShowtimeSummaryDto {

    private final Integer showtimeId;
    private final String movieTitle;
    private final String auditoriumName;
    private final LocalDateTime startTime;
    private final int totalSeats;
    private final int soldSeats;
    private final int heldSeats;
    private final int availableSeats;
}
