package com.cinema.hub.backend.dto.staff;

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
public class StaffShowtimeOptionDto {
    private Integer showtimeId;
    private String label;
    private String auditoriumName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
