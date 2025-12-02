package com.cinema.hub.backend.dto.showtime;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ShowtimeOccurrenceResponse(
        Integer id,
        LocalDate showDate,
        String showDateLabel,
        Integer auditoriumId,
        String auditoriumName,
        Boolean active,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean ended,
        Integer movieId,
        String movieTitle,
        String movieOriginalTitle) {
}
