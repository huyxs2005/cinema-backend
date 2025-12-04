package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.StaffBookingSummaryDto;
import com.cinema.hub.backend.dto.staff.UpdatePaymentMethodRequest;
import com.cinema.hub.backend.dto.staff.WalkInBookingRequest;
import com.cinema.hub.backend.payment.model.PaymentStatusResponse;
import com.cinema.hub.backend.payment.service.PaymentStatusService;
import com.cinema.hub.backend.service.UserService;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import com.cinema.hub.backend.util.TimeProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff/bookings")
@Validated
@RequiredArgsConstructor
public class StaffBookingController {

    private final StaffBookingService staffBookingService;
    private final UserService userService;
    private final PaymentStatusService paymentStatusService;

    @GetMapping("/{bookingCode}")
    public StaffBookingSummaryDto getBooking(@PathVariable String bookingCode) {
        return staffBookingService.getBookingByCode(bookingCode);
    }

    @GetMapping
    public List<StaffBookingSummaryDto> getBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime date,
            @RequestParam(required = false, defaultValue = "false") boolean pendingOnly) {
        if (pendingOnly) {
            return staffBookingService.getPendingBookingsToday();
        }
        if (date != null) {
            return staffBookingService.getBookingsByDate(date);
        }
        OffsetDateTime today = TimeProvider.now();
        return staffBookingService.getBookingsByDate(today);
    }

    @PostMapping
    public StaffBookingSummaryDto createWalkIn(@Valid @RequestBody WalkInBookingRequest request) {
        return staffBookingService.createWalkInBooking(request, userService.requireCurrentUser());
    }

    @PutMapping("/{bookingId}/payment-method")
    public StaffBookingSummaryDto updatePaymentMethod(@PathVariable Integer bookingId,
                                                      @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return staffBookingService.updatePaymentMethod(bookingId, request.getPaymentMethod());
    }

    @GetMapping("/{bookingId}/status")
    public PaymentStatusResponse getBookingStatus(@PathVariable Integer bookingId) {
        return paymentStatusService.getStatus(bookingId);
    }

}
