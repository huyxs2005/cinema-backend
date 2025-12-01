package com.cinema.hub.backend.web.view;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ShowtimeSlotView {
    private Integer auditoriumId;
    private Integer id;
    private String auditoriumName;
    private String cinemaName;
    private String format;
    private OffsetDateTime startTime;
}
