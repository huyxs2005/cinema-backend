package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.TicketScanResponse;
import com.cinema.hub.backend.service.UserService;
import com.cinema.hub.backend.service.staff.StaffTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(originPatterns = {"https://*", "http://localhost:*"}, allowCredentials = "true")
@RequiredArgsConstructor
public class StaffTicketController {

    private final StaffTicketService staffTicketService;
    private final UserService userService;

    @GetMapping("/check-ticket/{bookingCode}")
    public TicketScanResponse checkTicket(@PathVariable String bookingCode) {
        return staffTicketService.checkTicketByCode(bookingCode);
    }

    @GetMapping("/check-ticket/manual")
    public TicketScanResponse checkTicketManually(@RequestParam("value") String value) {
        return staffTicketService.checkTicketManually(value);
    }

    @GetMapping("/checkin/lookup")
    public TicketScanResponse lookupTicket(@RequestParam("value") String value) {
        return staffTicketService.checkTicketManually(value);
    }

    @PostMapping("/checkin/{bookingCode}")
    public TicketScanResponse checkIn(@PathVariable String bookingCode) {
        return staffTicketService.checkInBooking(bookingCode, userService.requireCurrentUser());
    }
}
