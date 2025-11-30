package com.cinema.hub.backend.dto.showtime;

import java.util.List;

public record ShowtimeDayGroupResponse(
        int dayOfWeek,
        String dayLabel,
        int totalShowtimes,
        List<ShowtimeOccurrenceResponse> showtimes) {
}
