package com.cinema.hub.backend.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieOptionDto {

    private int id;
    private String title;
    private String originalTitle;
    private String status;
    private String posterUrl;
    private LocalDate releaseDate;
    private LocalDate endDate;
}
