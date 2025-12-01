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
                               Model model) {
        var currentUser = userService.requireCurrentUser();
        model.addAttribute("checkout", null);
        model.addAttribute("fallbackShowtimeId", showtimeId);
        if (!StringUtils.hasText(holdToken)) {
            model.addAttribute("checkoutError", "Phiên giữ ghế đã hết hạn. Vui lòng chọn lại ghế.");
            return "checkout";
        }
        String trimmedToken = holdToken.trim();
        model.addAttribute("checkoutToken", trimmedToken);
        try {
            CheckoutPageView checkoutView =
                    seatReservationService.getCheckoutView(showtimeId, trimmedToken, currentUser.getId());
            model.addAttribute("checkout", checkoutView);
            model.addAttribute("currentUserEmail", currentUser.getEmail());
        } catch (SeatSelectionException ex) {
            log.warn("Checkout token rejected for showtime {} and user {}: {}", showtimeId, currentUser.getId(), ex.getMessage());
            model.addAttribute("checkoutError", ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error when preparing checkout view", ex);
            model.addAttribute("checkoutError", "Không thể tải thông tin thanh toán. Vui lòng thử lại.");
        }
        return "checkout";
    }
}
