package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.service.BookingService;
import com.cinema.hub.backend.service.ShowtimeService;
import com.cinema.hub.backend.service.SeatService;
import com.cinema.hub.backend.service.UserService;
import com.cinema.hub.backend.service.view.SeatLayoutView;
import com.cinema.hub.backend.web.view.SeatSelectionItemView;
import com.cinema.hub.backend.web.view.SeatSelectionShowtimeView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class SeatSelectionPageController {

    private final ShowtimeService showtimeService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/movies/seat-fragment/{showtimeId}")
    public String seatFragment(@PathVariable int showtimeId, Model model) {
        SeatSelectionShowtimeView showtime = showtimeService.getSeatSelectionDetails(showtimeId);
        SeatLayoutView layout = seatService.buildSeatLayout(showtimeId);
        model.addAttribute("showtimeDetails", showtime);
        model.addAttribute("seatMap", layout.rows());
        model.addAttribute("maxSeatsPerOrder", showtime.getMaxSeatsPerOrder());
        model.addAttribute("preselectedSeats", List.<SeatSelectionItemView>of());
        model.addAttribute("preselectedTotal", "0 ₫");
        return "fragments/seat-selection :: seatLayout";
    }

    @GetMapping("/movies/confirmation/{bookingCode}")
    public String confirmation(@PathVariable String bookingCode, Model model) {
        var currentUser = userService.requireCurrentUser();
        com.cinema.hub.backend.web.view.BookingConfirmationView confirmation =
                bookingService.getConfirmationViewForUser(bookingCode, currentUser);
        model.addAttribute("booking", confirmation);
        return "confirmation";
    }
}
