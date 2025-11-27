package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.service.staff.StaffDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final StaffDashboardService staffDashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("showtimes", staffDashboardService.getUpcomingShowtimes(3));
        return "staff/dashboard";
    }

    @GetMapping("/bookings/today")
    public String bookingsToday(Model model) {
        model.addAttribute("bookings", staffDashboardService.getTodayBookings());
        return "staff/bookings-today";
    }

    @GetMapping("/bookings/search")
    public String bookingsSearch(@RequestParam(value = "keyword", required = false) String keyword,
                                 Model model) {
        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("results", staffDashboardService.searchBookings(keyword));
        }
        model.addAttribute("keyword", keyword);
        return "staff/bookings-search";
    }
}
