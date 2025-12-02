package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.StaffShowtimeFilterDto;
import com.cinema.hub.backend.dto.staff.StaffShowtimeOptionDto;
import com.cinema.hub.backend.dto.staff.StaffShowtimeSummaryDto;
import com.cinema.hub.backend.service.staff.StaffShowtimeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/showtimes")
@Validated
@RequiredArgsConstructor
public class StaffShowtimeController {

    private final StaffShowtimeService staffShowtimeService;

    @GetMapping
    public List<StaffShowtimeSummaryDto> list(@Valid @ModelAttribute StaffShowtimeFilterDto filter) {
        return staffShowtimeService.getShowtimes(filter);
    }

    @GetMapping("/{showtimeId}")
    public StaffShowtimeSummaryDto get(@PathVariable int showtimeId) {
        return staffShowtimeService.getShowtime(showtimeId);
    }

    @GetMapping("/options")
    public List<StaffShowtimeOptionDto> options(@RequestParam(name = "days", defaultValue = "7") int days) {
        return staffShowtimeService.getUpcomingOptions(Math.max(days, 1));
    }
}
