package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.CancelBookingResponse;
import com.cinema.hub.backend.dto.CreateBookingRequest;
import com.cinema.hub.backend.dto.CreateBookingResponse;
import com.cinema.hub.backend.dto.SeatHoldRequest;
import com.cinema.hub.backend.dto.SeatHoldResponse;
import com.cinema.hub.backend.dto.SeatMapItemDto;
import com.cinema.hub.backend.service.SeatReservationService;
import com.cinema.hub.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class SeatReservationController {

    private final SeatReservationService seatReservationService;
    private final UserService userService;

    @GetMapping(ApiEndpoints.SeatReservation.SEAT_LAYOUT)
    public List<SeatMapItemDto> getSeatMap(@PathVariable int showtimeId) {
        return seatReservationService.getSeatMap(showtimeId);
    }

    @PostMapping(ApiEndpoints.SeatReservation.HOLD_SEATS)
    public SeatHoldResponse holdSeats(@PathVariable int showtimeId,
                                      @Valid @RequestBody SeatHoldRequest request) {
        request.setShowtimeId(showtimeId);
        request.setUserId(userService.requireCurrentUser().getId());
        return seatReservationService.holdSeats(request);
    }

    @DeleteMapping(ApiEndpoints.SeatReservation.RELEASE_HOLD)
    public void releaseHold(@PathVariable int showtimeId, @PathVariable String holdToken) {
        seatReservationService.releaseHold(holdToken);
    }

    @PostMapping(ApiEndpoints.SeatReservation.RELEASE_HOLD_BEACON)
    public void releaseHoldBeacon(@PathVariable int showtimeId, @PathVariable String holdToken) {
        seatReservationService.releaseHold(holdToken);
    }

    @PostMapping(ApiEndpoints.SeatReservation.RELEASE_USER_HOLDS)
    public void releaseUserHolds() {
        seatReservationService.releaseHoldsForUser(userService.requireCurrentUser().getId());
    }

    @PostMapping(ApiEndpoints.SeatReservation.CREATE_BOOKING)
    public CreateBookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        if (request.getUserId() == null) {
            request.setUserId(userService.requireCurrentUser().getId());
        }
        return seatReservationService.createBooking(request);
    }

    @PostMapping(ApiEndpoints.SeatReservation.CANCEL_BOOKING)
    public CancelBookingResponse cancelBooking(@PathVariable int bookingId) {
        return seatReservationService.cancelBooking(bookingId);
    }
}
