package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.MovieDetailDto;
import com.cinema.hub.backend.dto.MovieOptionDto;
import com.cinema.hub.backend.service.MovieService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/now-showing")
    public List<MovieDetailDto> getNowShowingMovies() {
        return movieService.getNowShowingMovies();
    }

    @GetMapping("/coming-soon")
    public List<MovieDetailDto> getComingSoonMovies() {
        return movieService.getComingSoonMovies();
    }

    @GetMapping("/options")
    public List<MovieOptionDto> getMovieOptions() {
        return movieService.getMovieOptions();
    }

    @GetMapping("/{id}")
    public MovieDetailDto getMovieDetail(@PathVariable int id) {
        try {
            return movieService.getMovieDetailById(id);
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
