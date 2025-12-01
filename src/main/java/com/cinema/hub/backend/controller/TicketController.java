package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.payment.service.PaymentService;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class TicketController {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentService paymentService;

    @GetMapping("/movies/tickets/{bookingCode}")
    public String ticketView(@PathVariable String bookingCode, Model model) {
        var booking = bookingRepository.findByBookingCodeWithShowtime(bookingCode)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        var seats = bookingSeatRepository.findDetailedByBooking(booking.getId());
        model.addAttribute("booking", booking);
        model.addAttribute("showtime", booking.getShowtime());
        model.addAttribute("seats", seats);
        model.addAttribute("qrUrl", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + bookingCode);
        return "ticket-view";
    }

    @GetMapping("/movies/tickets/{bookingCode}/download")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable String bookingCode) {
        var booking = bookingRepository.findByBookingCodeWithShowtime(bookingCode)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        byte[] pdf = paymentService.generateTicketPdf(booking.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + bookingCode + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
