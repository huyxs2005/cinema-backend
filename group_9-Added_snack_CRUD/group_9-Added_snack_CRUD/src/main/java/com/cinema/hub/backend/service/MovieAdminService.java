package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.admin.MovieAdminRequestDto;
import com.cinema.hub.backend.dto.admin.MovieAdminResponseDto;
import com.cinema.hub.backend.entity.Genre;
import com.cinema.hub.backend.entity.HomeBanner;
import com.cinema.hub.backend.entity.Movie;
import com.cinema.hub.backend.entity.MovieCredit;
import com.cinema.hub.backend.entity.MovieGenre;
import com.cinema.hub.backend.entity.MovieGenreId;
import com.cinema.hub.backend.entity.Person;
import com.cinema.hub.backend.repository.GenreRepository;
import com.cinema.hub.backend.repository.HomeBannerRepository;
import com.cinema.hub.backend.repository.MovieCreditRepository;
import com.cinema.hub.backend.repository.MovieGenreRepository;
import com.cinema.hub.backend.repository.MovieRepository;
import com.cinema.hub.backend.repository.PersonRepository;
import com.cinema.hub.backend.util.YoutubeEmbedHelper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieAdminService {

    private static final String CREDIT_ACTOR = "Actor";
    private static final String CREDIT_DIRECTOR = "Director";
    private static final String STATUS_NOW_SHOWING = "NowShowing";
    private static final String STATUS_COMING_SOON = "ComingSoon";
    private static final String STATUS_STOPPED = "Stopped";

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final PersonRepository personRepository;
    private final HomeBannerRepository homeBannerRepository;

    private static final Set<String> ALLOWED_GENRES = new HashSet<>(Arrays.asList(
            "Hành động",
            "Phiêu lưu",
            "Hoạt hình",
            "Hài",
            "Tội phạm",
            "Chính kịch",
            "Gia đình",
            "Kinh dị",
            "Ca nhạc",
            "Bí ẩn",
            "Thần thoại",
            "Tâm lý",
            "Tình cảm",
            "Khoa học viễn tưởng",
            "Hồi hộp (Suspense)",
            "Giật gân (Thriller)"
    ));

    @Transactional(readOnly = true)
    public List<MovieAdminResponseDto> getAll(String keyword, String status, String genre, String durationOrder) {
        List<MovieAdminResponseDto> movies = movieRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(this::mapToResponse)
                .toList();

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : null;
        String normalizedGenre = StringUtils.hasText(genre) ? genre.trim().toLowerCase(Locale.ROOT) : null;

        List<MovieAdminResponseDto> filtered = movies.stream()
                .filter(movie -> normalizedKeyword == null || matchesKeyword(movie, normalizedKeyword))
                .filter(movie -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(movie.getStatus()))
                .filter(movie -> normalizedGenre == null || matchesGenre(movie, normalizedGenre))
                .collect(Collectors.toList());

        if ("length_desc".equalsIgnoreCase(durationOrder)) {
            filtered.sort((a, b) -> compareDuration(b, a));
        } else if ("length_asc".equalsIgnoreCase(durationOrder)) {
            filtered.sort(this::compareDuration);
        }
        return filtered;
    }

    @Transactional(readOnly = true)
    public MovieAdminResponseDto getById(int id) {
        return mapToResponse(findMovie(id));
    }

    public MovieAdminResponseDto create(MovieAdminRequestDto dto) {
        Movie movie = new Movie();
        applyBasicFields(movie, dto);
        movie = movieRepository.save(movie);
        replaceGenres(movie, dto.getGenres());
        movieCreditRepository.deleteByMovie(movie);
        movieCreditRepository.flush();
        movie.getCredits().clear();
        replaceCredits(movie, dto.getDirectors(), CREDIT_DIRECTOR);
        replaceCredits(movie, dto.getActors(), CREDIT_ACTOR);
        return mapToResponse(fetchMovieWithRelations(movie.getId()));
    }

    public MovieAdminResponseDto update(int id, MovieAdminRequestDto dto) {
        Movie movie = findMovie(id);
        applyBasicFields(movie, dto);
        movie = movieRepository.save(movie);
        replaceGenres(movie, dto.getGenres());
        movieCreditRepository.deleteByMovie(movie);
        movieCreditRepository.flush();
        movie.getCredits().clear();
        replaceCredits(movie, dto.getDirectors(), CREDIT_DIRECTOR);
        replaceCredits(movie, dto.getActors(), CREDIT_ACTOR);
        return mapToResponse(fetchMovieWithRelations(movie.getId()));
    }

    public void delete(int id) {
        Movie movie = findMovie(id);
        detachMovieFromBanners(movie);
        movieGenreRepository.deleteByMovie(movie);
        movieCreditRepository.deleteByMovie(movie);
        movieRepository.delete(movie);
    }

    private Movie findMovie(int id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movie not found"));
    }

    private void applyBasicFields(Movie movie, MovieAdminRequestDto dto) {
        movie.setTitle(dto.getTitle());
        movie.setOriginalTitle(dto.getOriginalTitle());
        movie.setDescription(dto.getDescription());
        movie.setDurationMinutes(dto.getDurationMinutes());
        movie.setAgeRating(dto.getAgeRating());
        movie.setPosterUrl(dto.getPosterUrl());
        movie.setTrailerUrl(dto.getTrailerUrl());
        movie.setTrailerEmbedUrl(YoutubeEmbedHelper.normalize(dto.getTrailerEmbedUrl()));
        movie.setImdbUrl(dto.getImdbUrl());
        LocalDate release = dto.getReleaseDate();
        LocalDate endDate = dto.getEndDate();
        if (release != null && endDate != null && endDate.isBefore(release)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày ngưng chiếu phải lớn hơn hoặc bằng ngày khởi chiếu");
        }
        movie.setReleaseDate(release);
        movie.setEndDate(endDate);
        movie.setStatus(deriveStatus(release, endDate));
    }

    private void replaceGenres(Movie movie, List<String> requestedGenres) {
        movieGenreRepository.deleteByMovie(movie);
        List<String> names = sanitizeList(requestedGenres).stream()
                .filter(name -> ALLOWED_GENRES.contains(name))
                .toList();
        if (names.isEmpty()) {
            return;
        }
        List<MovieGenre> genres = new ArrayList<>();
        for (String name : names) {
            Genre genre = genreRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> genreRepository.save(Genre.builder().name(name).build()));
            genres.add(MovieGenre.builder()
                    .id(MovieGenreId.builder()
                            .movieId(movie.getId())
                            .genreId(genre.getId())
                            .build())
                    .movie(movie)
                    .genre(genre)
                    .build());
        }
        movieGenreRepository.saveAll(genres);
        movie.getMovieGenres().clear();
        movie.getMovieGenres().addAll(genres);
    }

    private void replaceCredits(Movie movie, List<String> names, String creditType) {
        List<String> sanitized = sanitizeList(names);
        if (sanitized.isEmpty()) {
            return;
        }
        int order = 1;
        List<MovieCredit> credits = new ArrayList<>();
        for (String fullName : sanitized) {
            Person person = personRepository.findByFullNameIgnoreCase(fullName)
                    .orElseGet(() -> personRepository.save(Person.builder().fullName(fullName).build()));
            credits.add(MovieCredit.builder()
                    .movie(movie)
                    .person(person)
                    .creditType(creditType)
                    .sortOrder(order++)
                    .build());
        }
        movieCreditRepository.saveAll(credits);
        // refresh association on entity
        movie.getCredits().removeIf(credit -> creditType.equalsIgnoreCase(credit.getCreditType()));
        movie.getCredits().addAll(credits);
    }

    private MovieAdminResponseDto mapToResponse(Movie movie) {
        return MovieAdminResponseDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .description(movie.getDescription())
                .durationMinutes(movie.getDurationMinutes())
                .ageRating(movie.getAgeRating())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .trailerEmbedUrl(movie.getTrailerEmbedUrl())
                .imdbUrl(movie.getImdbUrl())
                .status(movie.getStatus())
                .releaseDate(movie.getReleaseDate())
                .endDate(movie.getEndDate())
                .genres(movie.getMovieGenres().stream()
                        .map(mg -> mg.getGenre().getName())
                        .distinct()
                        .toList())
                .directors(extractCredits(movie, CREDIT_DIRECTOR))
                .actors(extractCredits(movie, CREDIT_ACTOR))
                .build();
    }

    private Movie fetchMovieWithRelations(Integer id) {
        return movieRepository.findMovieDetailById(id)
                .orElseGet(() -> movieRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Movie not found")));
    }

    private List<String> extractCredits(Movie movie, String creditType) {
        return movie.getCredits().stream()
                .filter(credit -> creditType.equalsIgnoreCase(credit.getCreditType()))
                .sorted((a, b) -> {
                    Integer o1 = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
                    Integer o2 = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
                    return o1.compareTo(o2);
                })
                .map(MovieCredit::getPerson)
                .filter(person -> person != null && person.getFullName() != null)
                .map(Person::getFullName)
                .distinct()
                .toList();
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .map(value -> value.replaceAll("\\s+", " "))
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    List<String> unique = new ArrayList<>();
                    for (String val : list) {
                        boolean exists = unique.stream()
                                .anyMatch(existing -> existing.equalsIgnoreCase(val));
                        if (!exists) {
                            unique.add(val);
                        }
                    }
                    return unique;
                }));
    }

    private void detachMovieFromBanners(Movie movie) {
        List<HomeBanner> banners = homeBannerRepository.findByMovieId(movie.getId());
        if (banners.isEmpty()) {
            return;
        }
        banners.forEach(banner -> banner.setMovieId(null));
        homeBannerRepository.saveAll(banners);
    }

    private String deriveStatus(LocalDate release, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate != null && endDate.isBefore(today)) {
            return STATUS_STOPPED;
        }
        if (release != null && release.isAfter(today)) {
            return STATUS_COMING_SOON;
        }
        return STATUS_NOW_SHOWING;
    }

    private boolean matchesKeyword(MovieAdminResponseDto movie, String normalized) {
        return containsIgnoreCase(movie.getTitle(), normalized)
                || containsIgnoreCase(movie.getOriginalTitle(), normalized);
    }

    private boolean containsIgnoreCase(String value, String normalized) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesGenre(MovieAdminResponseDto movie, String normalizedGenre) {
        if (movie.getGenres() == null || movie.getGenres().isEmpty()) {
            return false;
        }
        return movie.getGenres().stream()
                .filter(Objects::nonNull)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(name -> name.equals(normalizedGenre));
    }

    private int compareDuration(MovieAdminResponseDto a, MovieAdminResponseDto b) {
        int durationA = a.getDurationMinutes() == null ? 0 : a.getDurationMinutes();
        int durationB = b.getDurationMinutes() == null ? 0 : b.getDurationMinutes();
        return Integer.compare(durationA, durationB);
    }

}
