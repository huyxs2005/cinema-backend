package com.cinema.hub.backend.controller.staff;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staff")
public class StaffPageController {

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "staff/dashboard";
    }

    @GetMapping("/showtimes")
    public String showtimes() {
        return "staff/showtimes";
    }

    @GetMapping("/counter-booking")
    public String booking(@RequestParam(value = "showtimeId", required = false) Integer showtimeId,
                          Model model) {
        model.addAttribute("initialShowtimeId", showtimeId);
        return "staff/counter-booking";
    }

    @GetMapping("/qr-scan")
    public String scan() {
        return "staff/qr-scan";
    }

    @GetMapping("/booking")
    public String legacyBookingRedirect(@RequestParam(value = "showtimeId", required = false) Integer showtimeId) {
        if (showtimeId != null) {
            return "redirect:/staff/counter-booking?showtimeId=" + showtimeId;
        }
        return "redirect:/staff/counter-booking";
    }

    @GetMapping("/scan")
    public String legacyScanRedirect() {
        return "redirect:/staff/qr-scan";
    }

    @GetMapping("/portal")
    public String legacyPortalRedirect() {
        return "redirect:/staff/dashboard";
    }
}
