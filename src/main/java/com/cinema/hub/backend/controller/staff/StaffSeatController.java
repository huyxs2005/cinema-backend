package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.StaffSeatStatusDto;
import com.cinema.hub.backend.service.staff.StaffSeatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/showtimes/{showtimeId}/seats")
@Validated
@RequiredArgsConstructor
public class StaffSeatController {

    private final StaffSeatService staffSeatService;

    @GetMapping
    public List<StaffSeatStatusDto> list(@PathVariable int showtimeId) {
        return staffSeatService.getSeatStatuses(showtimeId);
    }
}
