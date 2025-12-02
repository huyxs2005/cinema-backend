package com.cinema.hub.backend.dto.showtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuditoriumOptionDto {

    private final Integer id;
    private final String name;
    private final Integer totalSeats;
}
