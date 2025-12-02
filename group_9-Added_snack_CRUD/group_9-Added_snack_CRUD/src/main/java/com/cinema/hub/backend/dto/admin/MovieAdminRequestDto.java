package com.cinema.hub.backend.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MovieAdminRequestDto {

    @NotBlank
    private String title;

    private String originalTitle;

    private String description;

    @NotNull
    @Min(1)
    @jakarta.validation.constraints.Max(900)
    private Integer durationMinutes;

    private String ageRating;

    private String posterUrl;

    private String trailerUrl;

    private String trailerEmbedUrl;

    private String imdbUrl;

    private LocalDate releaseDate;

    private LocalDate endDate;

    @Size(max = 50)
    private List<String> genres;

    @Size(max = 50)
    private List<String> directors;

    @Size(max = 50)
    private List<String> actors;
}
