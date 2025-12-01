package com.cinema.hub.backend.payment.service;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.payment.util.PaymentException;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEmailService {

    private static final DateTimeFormatter EMAIL_SHOWTIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd 'tháng' MM 'năm' yyyy");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PdfTicketGenerator pdfTicketGenerator;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Transactional(readOnly = true)
    public void sendTicket(Integer bookingId, String email) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getPaymentStatus() != PaymentStatus.Paid) {
            throw new PaymentException("Booking must be paid before sending tickets");
        }
        List<BookingSeat> seats = bookingSeatRepository.findDetailedByBooking(bookingId);
        byte[] pdfData = pdfTicketGenerator.generateTicket(booking, seats);
        sendEmailWithAttachment(email, booking, pdfData, seats);
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(Integer bookingId) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getPaymentStatus() != PaymentStatus.Paid) {
            throw new PaymentException("Booking must be paid before downloading tickets");
        }
        List<BookingSeat> seats = bookingSeatRepository.findDetailedByBooking(bookingId);
        return pdfTicketGenerator.generateTicket(booking, seats);
    }

    private void sendEmailWithAttachment(String recipient,
                                         Booking booking,
                                         byte[] pdfData,
                                         List<BookingSeat> seats) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(senderEmail);
            helper.setTo(recipient);
            helper.setSubject(buildSubject(booking));
            helper.setText(buildEmailBody(booking, seats), false);
            helper.addAttachment("ticket-" + booking.getBookingCode() + ".pdf", new ByteArrayResource(pdfData));
            mailSender.send(message);
            log.info("Ticket email sent for booking {} to {}", booking.getBookingCode(), recipient);
        } catch (MessagingException ex) {
            throw new PaymentException("Unable to send ticket email", ex);
        }
    }

    private String buildSubject(Booking booking) {
        return "Xác nhận thanh toán & Vé điện tử - [Mã vé: %s]".formatted(booking.getBookingCode());
    }

    private String buildEmailBody(Booking booking, List<BookingSeat> seats) {
        String seatLine = seats.stream()
                .map(this::formatSeat)
                .collect(Collectors.joining(", "));
        String auditorium = sanitizeAuditoriumName(booking.getShowtime().getAuditorium().getName());
        String showtime = EMAIL_SHOWTIME_FORMAT.format(booking.getShowtime().getStartTime());

        return """
                Kính gửi Quý khách,

                Hệ thống Cinema HUB xác nhận giao dịch của Quý khách đã thành công. Vé điện tử đã được đính kèm trong email này.

                Chi tiết đơn hàng:

                - Mã đặt vé: %s
                - Phim: %s
                - Suất chiếu: %s
                - Vị trí: Rạp Cinema HUB - Phòng %s / Ghế %s

                Quý khách vui lòng xuất trình email này tại quầy vé hoặc lối vào phòng chiếu.

                Cảm ơn Quý khách đã sử dụng dịch vụ.

                Trân trọng,
                Cinema HUB
                """.formatted(
                booking.getBookingCode(),
                booking.getShowtime().getMovie().getTitle(),
                showtime,
                auditorium,
                seatLine
        );
    }

    private String sanitizeAuditoriumName(String source) {
        if (source == null || source.isBlank()) {
            return "Hall 01";
        }
        return source.trim();
    }

    private String formatSeat(BookingSeat seat) {
        var seatEntity = seat.getShowtimeSeat().getSeat();
        var seatType = seatEntity.getSeatType() != null ? seatEntity.getSeatType().getName() : "Standard";
        return seatEntity.getRowLabel() + seatEntity.getSeatNumber() + " (" + seatType + ")";
    }
}
