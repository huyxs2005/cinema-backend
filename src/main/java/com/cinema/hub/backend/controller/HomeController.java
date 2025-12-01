package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.MovieDetailDto;
import com.cinema.hub.backend.service.MovieService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final MovieService movieService;

    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping("/movies/{id}")
    public String movieDetailPage(@PathVariable int id, Model model) {
        try {
            MovieDetailDto movie = movieService.getMovieDetailById(id);
            model.addAttribute("movie", movie);
            model.addAttribute("movieId", id);
            log.info("Loaded movie detail {} ({})", movie.getTitle(), movie.getId());
            return "movie-detail";
        } catch (EntityNotFoundException ex) {
            log.warn("Requested movie {} not found: {}", id, ex.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/ticket-prices")
    public String ticketPricesPage() {
        return "ticket-prices";
    }

    @GetMapping("/about")
    public String aboutPage() {
        return "about";
    }

    @GetMapping("/showtimes")
    public String showtimesPage() {
        return "showtimes";
    }

    @GetMapping("/faq")
    public String faqPage() {
        return "faq";
    }

}
