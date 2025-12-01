package com.cinema.hub.backend.payment.controller;

import com.cinema.hub.backend.service.BookingService;
import com.cinema.hub.backend.service.UserService;
import com.cinema.hub.backend.payment.model.PaymentCheckoutRequest;
import com.cinema.hub.backend.payment.model.PaymentCheckoutResponse;
import com.cinema.hub.backend.payment.model.PaymentCreateRequest;
import com.cinema.hub.backend.payment.model.PaymentStatusResponse;
import com.cinema.hub.backend.payment.model.SendTicketRequest;
import com.cinema.hub.backend.payment.service.PaymentService;
import com.cinema.hub.backend.payment.service.PaymentStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentStatusService paymentStatusService;
    private final BookingService bookingService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<PaymentCheckoutResponse> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        return ResponseEntity.ok(paymentService.createVietQR(request));
    }

    @PostMapping("/payos/checkout")
    public ResponseEntity<PaymentCheckoutResponse> legacyCheckout(@Valid @RequestBody PaymentCheckoutRequest request) {
        var user = userService.requireCurrentUser();
        var booking = request.getBookingId() != null
                ? bookingService.getBookingForUser(user, request.getBookingId())
                : bookingService.createBookingFromHold(user, request.getHoldToken(), "VietQR");

        PaymentCreateRequest createRequest = new PaymentCreateRequest();
        createRequest.setBookingId(booking.getId());
        String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : user.getEmail();
        createRequest.setEmail(email);
        PaymentCheckoutResponse response = paymentService.createVietQR(createRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{bookingId}")
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable("bookingId") Integer bookingId) {
        return ResponseEntity.ok(paymentStatusService.getStatus(bookingId));
    }

    @PostMapping("/send-ticket")
    public ResponseEntity<Void> sendTicket(@Valid @RequestBody SendTicketRequest request) {
        paymentService.sendTicketEmail(request.getBookingId(), request.getEmail());
        return ResponseEntity.accepted().build();
    }
}
