package com.cinema.hub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDetailDto {
    private int id;
    private String title;
    private String originalTitle;
    private String description;
    private Integer durationMinutes;
    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private String trailerEmbedUrl;
    private String imdbUrl;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private List<String> genres;
    private List<String> actors;
    private List<String> directors;
}
