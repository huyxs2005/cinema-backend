package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.booking.StaffBookingStatusResponse;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatHoldRequest;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatHoldResponse;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatMapView;
import com.cinema.hub.backend.security.UserPrincipal;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/api")
@RequiredArgsConstructor
public class StaffBookingRestController {

    private final StaffBookingService staffBookingService;

    @GetMapping("/showtimes/{showtimeId}/seats")
    public StaffSeatMapView getSeatMap(@PathVariable Integer showtimeId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return staffBookingService.loadSeatMapForShowtime(
                showtimeId, principal != null ? principal.getUser() : null);
    }

    @PostMapping("/showtimes/{showtimeId}/holds")
    public StaffSeatHoldResponse holdSeats(@PathVariable Integer showtimeId,
                                           @RequestBody(required = false) StaffSeatHoldRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        var held = staffBookingService.holdSeats(
                showtimeId,
                request != null ? request.getSeatIds() : List.of(),
                principal != null ? principal.getUser() : null);
        return StaffSeatHoldResponse.builder()
                .seatIds(held)
                .status("HELD")
                .build();
    }

    @PostMapping("/showtimes/{showtimeId}/holds/release")
    public StaffSeatHoldResponse releaseHolds(@PathVariable Integer showtimeId,
                                              @RequestBody(required = false) StaffSeatHoldRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        staffBookingService.releaseSeatHolds(
                showtimeId,
                request != null ? request.getSeatIds() : List.of(),
                principal != null ? principal.getUser() : null);
        return StaffSeatHoldResponse.builder()
                .seatIds(request != null ? request.getSeatIds() : List.of())
                .status("RELEASED")
                .build();
    }

    @GetMapping("/bookings/{bookingCode}/status")
    public StaffBookingStatusResponse getStatus(@PathVariable String bookingCode) {
        return staffBookingService.getBookingStatus(bookingCode);
    }
}
