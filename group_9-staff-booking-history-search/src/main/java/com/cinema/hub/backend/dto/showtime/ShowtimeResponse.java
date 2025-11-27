package com.cinema.hub.backend.dto.showtime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShowtimeResponse {

    private final Integer id;
    private final Integer movieId;
    private final String movieTitle;
    private final Integer auditoriumId;
    private final String auditoriumName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final BigDecimal basePrice;
    private final Boolean active;
}
