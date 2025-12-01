package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.service.SeatReservationService;
import com.cinema.hub.backend.service.UserService;
import com.cinema.hub.backend.service.exception.SeatSelectionException;
import com.cinema.hub.backend.web.view.CheckoutPageView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final SeatReservationService seatReservationService;
    private final UserService userService;

    @GetMapping("/checkout/{showtimeId}")
    public String checkoutPage(@PathVariable int showtimeId,
                               @RequestParam(name = "token", required = false) String holdToken,
                               @RequestParam(name = "bookingId", required = false) Integer bookingId,
                               Model model) {
        var currentUser = userService.requireCurrentUser();
        model.addAttribute("checkout", null);
        model.addAttribute("fallbackShowtimeId", showtimeId);
        String trimmedToken = StringUtils.hasText(holdToken) ? holdToken.trim() : null;
        model.addAttribute("checkoutToken", trimmedToken);
        try {
            CheckoutPageView checkoutView;
            if (StringUtils.hasText(trimmedToken)) {
                checkoutView = seatReservationService.getCheckoutView(showtimeId, trimmedToken, currentUser.getId());
            } else if (bookingId != null) {
                checkoutView = seatReservationService.getCheckoutViewForBooking(bookingId, currentUser.getId());
            } else {
                model.addAttribute("checkoutError", "PhiA�n gi��_ gh��� �`A� h���t h���n. Vui lA�ng ch��?n gh��� l���i t��� �`��u.");
                return "checkout";
            }
            model.addAttribute("checkout", checkoutView);
            model.addAttribute("currentUserEmail", currentUser.getEmail());
            model.addAttribute("checkoutBookingId", checkoutView.getBookingId());
            model.addAttribute("checkoutBookingCode", checkoutView.getBookingCode());
        } catch (SeatSelectionException ex) {
            log.warn("Checkout token rejected for showtime {} and user {}: {}", showtimeId, currentUser.getId(), ex.getMessage());
            model.addAttribute("checkoutError", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error when preparing checkout view", ex);
            model.addAttribute("checkoutError", "KhA'ng th��� t���i thA'ng tin thanh toA�n. Vui lA�ng th��- l���i.");
        }
        return "checkout";
    }
}
