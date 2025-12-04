package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.dto.staff.StaffPayosCheckoutRequest;
import com.cinema.hub.backend.payment.model.PaymentCheckoutResponse;
import com.cinema.hub.backend.payment.model.PaymentCreateRequest;
import com.cinema.hub.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/payment")
@RequiredArgsConstructor
public class StaffPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/payos/checkout")
    public PaymentCheckoutResponse createStaffPayosCheckout(@PathVariable Integer bookingId,
                                                            @Valid @RequestBody StaffPayosCheckoutRequest request) {
        PaymentCreateRequest createRequest = new PaymentCreateRequest();
        createRequest.setBookingId(bookingId);
        createRequest.setEmail(request.getEmail().trim());
        return paymentService.createVietQR(createRequest);
    }
}
