package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.MovieDetailDto;
import com.cinema.hub.backend.dto.MovieOptionDto;
import com.cinema.hub.backend.entity.Movie;
import com.cinema.hub.backend.entity.MovieCredit;
import com.cinema.hub.backend.entity.MovieGenre;
import com.cinema.hub.backend.repository.MovieRepository;
import com.cinema.hub.backend.util.YoutubeEmbedHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieService {

    private static final String STATUS_NOW_SHOWING = "NowShowing";
    private static final String STATUS_COMING_SOON = "ComingSoon";
    private final MovieRepository movieRepository;

    public List<MovieDetailDto> getNowShowingMovies() {
        LocalDate today = LocalDate.now();
        return movieRepository.findAllWithGenres()
                .stream()
                .filter(movie -> isNowShowing(movie, today))
                .map(this::mapToSummaryDto)
                .toList();
    }

    public List<MovieDetailDto> getComingSoonMovies() {
        LocalDate today = LocalDate.now();
        return movieRepository.findAllWithGenres()
                .stream()
                .filter(movie -> isComingSoon(movie, today))
                .map(this::mapToSummaryDto)
                .toList();
    }

    public MovieDetailDto getMovieDetailById(int movieId) {
        Movie movie = movieRepository.findMovieDetailById(movieId)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found"));
        return mapToDetailDto(movie);
    }

    public List<MovieOptionDto> getMovieOptions() {
        LocalDate today = LocalDate.now();
        return movieRepository.findAll().stream()
                .map(movie -> new MovieStatusPair(movie, determineStatus(movie, today)))
                .filter(pair -> pair.status().isPresent())
                .map(pair -> MovieOptionDto.builder()
                        .id(pair.movie().getId())
                        .title(pair.movie().getTitle())
                        .originalTitle(pair.movie().getOriginalTitle())
                        .status(pair.status().get())
                        .posterUrl(pair.movie().getPosterUrl())
                        .build())
                .toList();
    }

    private MovieDetailDto mapToSummaryDto(Movie movie) {
        return MovieDetailDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes())
                .ageRating(movie.getAgeRating())
                .ageRatingDescription(describeAgeRating(movie.getAgeRating()))
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .trailerEmbedUrl(YoutubeEmbedHelper.normalize(movie.getTrailerEmbedUrl()))
                .imdbUrl(movie.getImdbUrl())
                .releaseDate(movie.getReleaseDate())
                .endDate(movie.getEndDate())
                .genres(extractGenres(movie))
                .actors(List.of())
                .directors(List.of())
                .build();
    }

    private MovieDetailDto mapToDetailDto(Movie movie) {
        return MovieDetailDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes())
                .ageRating(movie.getAgeRating())
                .ageRatingDescription(describeAgeRating(movie.getAgeRating()))
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .trailerEmbedUrl(YoutubeEmbedHelper.normalize(movie.getTrailerEmbedUrl()))
                .imdbUrl(movie.getImdbUrl())
                .releaseDate(movie.getReleaseDate())
                .endDate(movie.getEndDate())
                .genres(extractGenres(movie))
                .actors(extractCredits(movie, "Actor"))
                .directors(extractCredits(movie, "Director"))
                .build();
    }

    private List<String> extractGenres(Movie movie) {
        var movieGenres = movie.getMovieGenres();
        if (movieGenres == null) {
            return List.of();
        }

        return movieGenres.stream()
                .map(MovieGenre::getGenre)
                .filter(Objects::nonNull)
                .map(genre -> genre.getName())
                .distinct()
                .toList();
    }

    private List<String> extractCredits(Movie movie, String creditType) {
        var credits = movie.getCredits();
        if (credits == null) {
            return List.of();
        }

        Comparator<MovieCredit> comparator = Comparator.comparing(
                MovieCredit::getSortOrder,
                Comparator.nullsLast(Integer::compareTo)
        );

        return credits.stream()
                .filter(credit -> creditType.equalsIgnoreCase(credit.getCreditType()))
                .sorted(comparator)
                .map(MovieCredit::getPerson)
                .filter(Objects::nonNull)
                .map(person -> person.getFullName())
                .distinct()
                .toList();
    }

    private boolean isNowShowing(Movie movie, LocalDate today) {
        LocalDate release = movie.getReleaseDate();
        if (release == null || release.isAfter(today)) {
            return false;
        }
        LocalDate endDate = movie.getEndDate();
        return endDate == null || !endDate.isBefore(today);
    }

    private boolean isComingSoon(Movie movie, LocalDate today) {
        LocalDate release = movie.getReleaseDate();
        return release != null && release.isAfter(today);
    }

    private Optional<String> determineStatus(Movie movie, LocalDate today) {
        if (isNowShowing(movie, today)) {
            return Optional.of(STATUS_NOW_SHOWING);
        }
        if (isComingSoon(movie, today)) {
            return Optional.of(STATUS_COMING_SOON);
        }
        return Optional.empty();
    }

    private record MovieStatusPair(Movie movie, Optional<String> status) {}

    private String describeAgeRating(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase()) {
            case "P" -> "P - Phim được phép phổ biến đến người xem ở mọi độ tuổi.";
            case "K" -> "K - Phim được phổ biến đến người xem dưới 13 tuổi và có người bảo hộ đi kèm.";
            case "T13" -> "T13 - Phim được phổ biến đến người xem từ đủ 13 tuổi trở lên (13+).";
            case "T16" -> "T16 - Phim được phổ biến đến người xem từ đủ 16 tuổi trở lên (16+).";
            case "T18" -> "T18 - Phim được phổ biến đến người xem từ đủ 18 tuổi trở lên (18+).";
            default -> null;
        };
    }

}
