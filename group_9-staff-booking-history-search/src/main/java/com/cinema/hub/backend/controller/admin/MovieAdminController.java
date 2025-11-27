package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.admin.MovieAdminRequestDto;
import com.cinema.hub.backend.dto.admin.MovieAdminResponseDto;
import com.cinema.hub.backend.service.MovieAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class MovieAdminController {

    private final MovieAdminService movieAdminService;

    @GetMapping
    public List<MovieAdminResponseDto> getMovies() {
        return movieAdminService.getAll();
    }

    @GetMapping("/{id}")
    public MovieAdminResponseDto getMovie(@PathVariable int id) {
        return movieAdminService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieAdminResponseDto createMovie(@Valid @RequestBody MovieAdminRequestDto request) {
        return movieAdminService.create(request);
    }

    @PutMapping("/{id}")
    public MovieAdminResponseDto updateMovie(@PathVariable int id,
                                             @Valid @RequestBody MovieAdminRequestDto request) {
        return movieAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable int id) {
        movieAdminService.delete(id);
    }
}

