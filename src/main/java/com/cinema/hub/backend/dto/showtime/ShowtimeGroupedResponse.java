package com.cinema.hub.backend.dto.showtime;

import java.time.LocalTime;
import java.util.List;

public record ShowtimeGroupedResponse(
        LocalTime startTime,
        String startTimeLabel,
        int totalShowtimes,
        List<ShowtimeDayGroupResponse> dayGroups) {
}
