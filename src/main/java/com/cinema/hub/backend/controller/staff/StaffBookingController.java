package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.StaffBookingSummaryDto;
import com.cinema.hub.backend.dto.staff.WalkInBookingRequest;
import com.cinema.hub.backend.service.UserService;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/bookings")
@Validated
@RequiredArgsConstructor
public class StaffBookingController {

    private final StaffBookingService staffBookingService;
    private final UserService userService;

    @GetMapping("/{bookingCode}")
    public StaffBookingSummaryDto getBooking(@PathVariable String bookingCode) {
        return staffBookingService.getBookingByCode(bookingCode);
    }

    @PostMapping
    public StaffBookingSummaryDto createWalkIn(@Valid @RequestBody WalkInBookingRequest request) {
        return staffBookingService.createWalkInBooking(request, userService.requireCurrentUser());
    }
}
