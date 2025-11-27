package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.booking.StaffBookingDetailView;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingRequest;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingResult;
import com.cinema.hub.backend.dto.staff.booking.StaffComboSelection;
import com.cinema.hub.backend.dto.staff.booking.StaffPaymentMethod;
import com.cinema.hub.backend.dto.staff.booking.StaffVietQrInfo;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatMapView;
import com.cinema.hub.backend.security.UserPrincipal;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final StaffBookingService staffBookingService;

    @GetMapping("/bookings/new")
    public String newBooking(@RequestParam("showtimeId") Integer showtimeId,
                             Model model,
                             @AuthenticationPrincipal UserPrincipal principal) {
        StaffSeatMapView seatMap = staffBookingService.loadSeatMapForShowtime(
                showtimeId, principal != null ? principal.getUser() : null);
        model.addAttribute("seatMap", seatMap);
        model.addAttribute("combos", staffBookingService.getActiveCombos());
        return "staff/seat-selection";
    }

    @PostMapping("/bookings")
    public String createBooking(@RequestParam("showtimeId") Integer showtimeId,
                                @RequestParam("selectedSeats") String selectedSeats,
                                @RequestParam("paymentMethod") String paymentMethod,
                                @RequestParam(value = "customerEmail", required = false) String customerEmail,
                                @RequestParam(value = "customerPhone", required = false) String customerPhone,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserPrincipal principal) {
        List<Integer> seatIds = parseSeatIds(selectedSeats);
        List<StaffComboSelection> comboSelections = parseCombos(request.getParameterMap());
        StaffBookingRequest bookingRequest = StaffBookingRequest.builder()
                .showtimeId(showtimeId)
                .seatIds(seatIds)
                .comboSelections(comboSelections)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .paymentMethod(StaffPaymentMethod.from(paymentMethod))
                .build();
        StaffBookingResult result;
        try {
            result = staffBookingService.createBookingForStaff(
                    bookingRequest,
                    principal != null ? principal.getUser() : null);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        ex.getReason() != null ? ex.getReason() : "Ghế đã được đặt, vui lòng chọn ghế khác.");
                return "redirect:/staff/bookings/new?showtimeId=" + showtimeId;
            }
            throw ex;
        }
        if (result.getPaymentMethod() == StaffPaymentMethod.VIETQR) {
            redirectAttributes.addFlashAttribute("infoMessage", "Đơn đã tạo, hãy xác nhận thanh toán VietQR.");
            return "redirect:/staff/bookings/" + result.getBookingCode() + "/qr";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Thanh toán tiền mặt thành công.");
        return "redirect:/staff/bookings/" + result.getBookingCode();
    }

    @GetMapping("/bookings/{bookingCode}")
    public String bookingDetail(@PathVariable String bookingCode, Model model) {
        StaffBookingDetailView detailView = staffBookingService.getBookingDetail(bookingCode);
        model.addAttribute("booking", detailView);
        return "staff/booking-detail";
    }

    @GetMapping("/bookings/{bookingCode}/qr")
    public String bookingQr(@PathVariable String bookingCode, Model model) {
        StaffVietQrInfo qrInfo = staffBookingService.getVietQrInfo(bookingCode);
        model.addAttribute("qr", qrInfo);
        StaffBookingDetailView detail = staffBookingService.getBookingDetail(bookingCode);
        model.addAttribute("booking", detail);
        return "staff/booking-qr";
    }

    @PostMapping("/bookings/{bookingCode}/mark-paid")
    public String markPaid(@PathVariable String bookingCode,
                           RedirectAttributes redirectAttributes,
                           @AuthenticationPrincipal UserPrincipal principal) {
        staffBookingService.markBookingPaid(bookingCode, principal != null ? principal.getUser() : null);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận thanh toán.");
        return "redirect:/staff/bookings/" + bookingCode;
    }

    @PostMapping("/bookings/{bookingCode}/cancel")
    public String cancelBooking(@PathVariable String bookingCode,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserPrincipal principal) {
        staffBookingService.cancelBooking(bookingCode, principal != null ? principal.getUser() : null);
        redirectAttributes.addFlashAttribute("warningMessage", "Đã huỷ đơn đặt vé.");
        return "redirect:/staff/bookings/today";
    }

    private List<Integer> parseSeatIds(String rawSeatIds) {
        if (rawSeatIds == null || rawSeatIds.isBlank()) {
            return List.of();
        }
        String[] tokens = rawSeatIds.split(",");
        List<Integer> result = new ArrayList<>();
        for (String token : tokens) {
            try {
                result.add(Integer.parseInt(token.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    private List<StaffComboSelection> parseCombos(Map<String, String[]> parameterMap) {
        return parameterMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("combo_"))
                .map(entry -> {
                    try {
                        Integer comboId = Integer.parseInt(entry.getKey().substring(6));
                        String[] values = entry.getValue();
                        String raw = values != null && values.length > 0 ? values[0] : "0";
                        Integer quantity = Integer.parseInt(raw);
                        return StaffComboSelection.builder()
                                .comboId(comboId)
                                .quantity(quantity)
                                .build();
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(selection -> selection != null && selection.getQuantity() != null && selection.getQuantity() > 0)
                .collect(Collectors.toList());
    }
}
