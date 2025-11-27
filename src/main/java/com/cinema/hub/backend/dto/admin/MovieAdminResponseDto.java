package com.cinema.hub.backend.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class MovieAdminResponseDto {

    private final Integer id;
    private final String title;
    private final String originalTitle;
    private final String description;
    private final Integer durationMinutes;
    private final String ageRating;
    private final String posterUrl;
    private final String trailerUrl;
    private final String trailerEmbedUrl;
    private final String imdbUrl;
    private final String status;
    private final LocalDate releaseDate;
    private final LocalDate endDate;
    private final List<String> genres;
    private final List<String> directors;
    private final List<String> actors;
}
