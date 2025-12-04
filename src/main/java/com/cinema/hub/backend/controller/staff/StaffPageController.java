package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffPageController {

    private final StaffBookingService staffBookingService;

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

    @GetMapping("/pending-tickets")
    public String pendingTickets(@RequestParam(value = "focus", required = false) String focusCode,
                                 Model model) {
        Integer focusId = null;
        if (focusCode != null && !focusCode.isEmpty()) {
            try {
                // Try to parse as Integer first (for backward compatibility)
                focusId = Integer.parseInt(focusCode);
            } catch (NumberFormatException e) {
                // If not a number, treat as booking code and find the booking ID
                try {
                    var booking = staffBookingService.getBookingByCode(focusCode);
                    if (booking != null) {
                        focusId = booking.getBookingId();
                    }
                } catch (Exception ex) {
                    log.warn("Unable to find booking for code: {}", focusCode);
                }
            }
        }
        return renderPendingTickets(focusId, model);
    }

    @GetMapping("/pending-tickets/{bookingId}")
    public String pendingTicketDetail(@PathVariable Integer bookingId, Model model) {
        return renderPendingTickets(bookingId, model);
    }

    private String renderPendingTickets(Integer focusId, Model model) {
        model.addAttribute("bookings",
                staffBookingService.getPendingBookingsToday());
        model.addAttribute("focusBookingId", focusId);
        return "staff/pending-tickets";
    }

    @GetMapping("/sold-tickets")
    public String soldTickets(Model model) {
        model.addAttribute("bookings",
                staffBookingService.getBookingsByStatus(BookingStatus.Confirmed));
        return "staff/sold-tickets";
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

    @GetMapping("/bookings/{bookingCode}")
    public String bookingDetail(@PathVariable String bookingCode, Model model) {
        try {
            log.info("Loading booking detail for code: {}", bookingCode);
            var booking = staffBookingService.getBookingByCode(bookingCode);
            if (booking == null) {
                throw new jakarta.persistence.EntityNotFoundException("Booking not found: " + bookingCode);
            }
            model.addAttribute("booking", booking);
            return "staff/booking-confirmation";
        } catch (Exception ex) {
            log.error("Error loading booking detail for code {}: {}", bookingCode, ex.getMessage(), ex);
            model.addAttribute("error", "Không thể tải thông tin đơn hàng: " + ex.getMessage());
            return "staff/booking-confirmation";
        }
    }

    @GetMapping("/bookings/{bookingCode}/qr")
    public String bookingQr(@PathVariable String bookingCode, Model model) {
        try {
            log.info("Loading QR info for booking code: {}", bookingCode);
            var qr = staffBookingService.getBookingQrInfo(bookingCode);
            model.addAttribute("qr", qr);
            return "staff/booking-qr";
        } catch (Exception ex) {
            log.error("Error loading QR info for code {}: {}", bookingCode, ex.getMessage(), ex);
            model.addAttribute("error", "Không thể tải thông tin VietQR: " + ex.getMessage());
            return "staff/booking-qr";
        }
    }

    @PostMapping("/bookings/{bookingCode}/mark-paid")
    public String markPaid(@PathVariable String bookingCode) {
        try {
            var booking = staffBookingService.getBookingByCode(bookingCode);
            staffBookingService.verifyBooking(booking.getBookingId());
            return "redirect:/staff/bookings/" + bookingCode + "?success=paid";
        } catch (Exception ex) {
            log.error("Error marking booking as paid: {}", ex.getMessage(), ex);
            return "redirect:/staff/bookings/" + bookingCode + "?error=" + ex.getMessage();
        }
    }

    @PostMapping("/bookings/{bookingCode}/cancel")
    public String cancelBooking(@PathVariable String bookingCode) {
        try {
            var booking = staffBookingService.getBookingByCode(bookingCode);
            staffBookingService.cancelBooking(booking.getBookingId());
            return "redirect:/staff/pending-tickets?cancelled=" + bookingCode;
        } catch (Exception ex) {
            log.error("Error cancelling booking: {}", ex.getMessage(), ex);
            return "redirect:/staff/bookings/" + bookingCode + "?error=" + ex.getMessage();
        }
    }

    @GetMapping("/booking-confirmation/{bookingCode}")
    public String legacyBookingConfirmation(@PathVariable String bookingCode) {
        return "redirect:/staff/bookings/" + bookingCode;
    }
}
