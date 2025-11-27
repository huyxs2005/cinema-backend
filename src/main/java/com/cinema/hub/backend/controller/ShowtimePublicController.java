package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.showtime.ShowtimeResponse;
import com.cinema.hub.backend.service.ShowtimeService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimePublicController {

    private final ShowtimeService showtimeService;

    @GetMapping("/movie/{movieId}")
    public List<ShowtimeResponse> getUpcomingShowtimes(@PathVariable int movieId,
                                                       @RequestParam(required = false)
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                       LocalDate from,
                                                       @RequestParam(required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                        LocalDate to) {
        return showtimeService.getUpcomingShowtimesForMovie(movieId, from, to);
    }

    @GetMapping("/day")
    public List<ShowtimeResponse> getShowtimesByDay(@RequestParam
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                    LocalDate date) {
        return showtimeService.getShowtimesByDate(date);
    }
}
