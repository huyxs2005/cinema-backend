package com.cinema.hub.backend.dto.staff;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffShowtimeSummaryDto {
    private Integer showtimeId;
    private Integer movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean active;
    private long totalSeats;
    private long sellableSeats;
    private long soldSeats;
    private long heldSeats;
    private long availableSeats;
    private BigDecimal occupancyPercent;
}
