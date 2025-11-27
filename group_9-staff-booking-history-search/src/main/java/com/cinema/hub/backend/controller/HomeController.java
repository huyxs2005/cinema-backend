package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.MovieDetailDto;
import com.cinema.hub.backend.service.MovieService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
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
            return "movie-detail";
        } catch (EntityNotFoundException ex) {
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

}
