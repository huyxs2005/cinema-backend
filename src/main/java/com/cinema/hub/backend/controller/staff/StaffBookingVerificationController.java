package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.StaffBookingSummaryDto;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/bookings")
@Validated
@RequiredArgsConstructor
public class StaffBookingVerificationController {

    private final StaffBookingService staffBookingService;

    @PostMapping("/{bookingId}/verify")
    public StaffBookingSummaryDto verify(@PathVariable Integer bookingId) {
        return staffBookingService.verifyBooking(bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    public StaffBookingSummaryDto cancel(@PathVariable Integer bookingId) {
        return staffBookingService.cancelBooking(bookingId);
    }
}
