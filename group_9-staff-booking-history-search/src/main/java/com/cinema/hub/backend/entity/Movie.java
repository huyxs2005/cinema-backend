package com.cinema.hub.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "Movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MovieId")
    private Integer id;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "OriginalTitle")
    private String originalTitle;

    @Column(name = "Description")
    private String description;

    @Column(name = "DurationMinutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "AgeRating")
    private String ageRating;

    @Column(name = "PosterUrl")
    private String posterUrl;

    @Column(name = "TrailerUrl")
    private String trailerUrl;

    @Column(name = "TrailerEmbedUrl")
    private String trailerEmbedUrl;

    @Column(name = "ImdbUrl")
    private String imdbUrl;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "ReleaseDate")
    private LocalDate releaseDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<MovieGenre> movieGenres = new LinkedHashSet<>();

    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<MovieCredit> credits = new LinkedHashSet<>();
}
