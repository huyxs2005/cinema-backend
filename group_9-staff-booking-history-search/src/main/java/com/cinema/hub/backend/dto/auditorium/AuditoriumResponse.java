package com.cinema.hub.backend.dto.auditorium;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditoriumResponse {

    private final Integer id;
    private final String name;
    private final Integer numberOfRows;
    private final Integer numberOfColumns;
    private final Boolean active;
}
