package com.cinema.hub.backend.dto.showtime;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowtimeRequest {

    @NotNull(message = "Movie is required")
    private Integer movieId;

    @NotNull(message = "Auditorium is required")
    private Integer auditoriumId;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "Active flag is required")
    private Boolean active;

    private Integer cleanupMinutes;

    private String repeatMode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate repeatUntil;

    private List<Integer> repeatDays;
}
