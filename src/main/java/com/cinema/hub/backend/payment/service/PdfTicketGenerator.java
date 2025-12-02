package com.cinema.hub.backend.payment.service;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.payment.util.PaymentException;
import com.cinema.hub.backend.util.AsciiSanitizer;
import com.cinema.hub.backend.util.CurrencyFormatter;
import com.cinema.hub.backend.util.SeatTypeLabelResolver;
import com.cinema.hub.backend.util.TimeProvider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
@RequiredArgsConstructor
public class PdfTicketGenerator {

    private static final DateTimeFormatter SHOWTIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
    private static final DateTimeFormatter ISSUED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SpringTemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;

    public byte[] generateTicket(Booking booking, List<BookingSeat> seats) {
        try {
            TicketPdfModel payload = buildPayload(booking, seats);
            Context context = new Context();
            context.setVariable("ticket", payload);
            String html = templateEngine.process("ticket-pdf", context);
            return renderPdf(html);
        } catch (Exception ex) {
            throw new PaymentException("Unable to generate ticket PDF", ex);
        }
    }

    private byte[] renderPdf(String html) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            registerFont(builder, "classpath:fonts/Roboto-Regular.ttf", "Roboto", 400);
            registerFont(builder, "classpath:fonts/Roboto-Bold.ttf", "Roboto", 700);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        }
    }

    private void registerFont(PdfRendererBuilder builder,
                              String resourcePath,
                              String family,
                              int weight) {
        Resource resource = resourceLoader.getResource(resourcePath);
        try {
            byte[] fontBytes = resource.getInputStream().readAllBytes();
            builder.useFont(() -> new ByteArrayInputStream(fontBytes),
                    family,
                    weight,
                    BaseRendererBuilder.FontStyle.NORMAL,
                    true);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private TicketPdfModel buildPayload(Booking booking, List<BookingSeat> seats) throws IOException {
        String movieTitle = booking.getShowtime().getMovie().getTitle();
        String originalTitle = booking.getShowtime().getMovie().getOriginalTitle();
        String showtime = SHOWTIME_FORMAT.format(booking.getShowtime().getStartTime());
        OffsetDateTime issuedAt = booking.getPaidAt() != null ? booking.getPaidAt() : TimeProvider.now();
        BigDecimal total = booking.getFinalAmount() != null ? booking.getFinalAmount() : booking.getTotalAmount();
        List<String> seatGroups = buildSeatGroupLines(seats);

        String auditoriumName = sanitizeAuditoriumName(
                booking.getShowtime().getAuditorium().getName());

        return TicketPdfModel.builder()
                .movieTitle(ascii(movieTitle).toUpperCase(Locale.ROOT))
                .originalTitle(ascii(originalTitle))
                .bookingCode(ascii(booking.getBookingCode()))
                .cinemaName("Cinema HUB")
                .auditoriumName(ascii(auditoriumName))
                .showtime(ascii(showtime))
                .issuedAt(ascii(ISSUED_AT_FORMAT.format(issuedAt)))
                .total(ascii(CurrencyFormatter.format(total)))
                .seatGroups(seatGroups)
                .qrDataUrl(renderQrDataUri(booking.getBookingCode()))
                .build();
    }

    private String sanitizeAuditoriumName(String source) {
        if (source == null || source.isBlank()) {
            return "Hall 01";
        }
        String normalized = source.trim();
        return normalized.length() > 2 ? normalized : "Hall 01";
    }

    private String renderQrDataUri(String bookingCode) throws IOException {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode("BOOKING:" + bookingCode, BarcodeFormat.QR_CODE, 340, 340);
            try (ByteArrayOutputStream qrStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrStream);
                String base64 = Base64.getEncoder().encodeToString(qrStream.toByteArray());
                return "data:image/png;base64," + base64;
            }
        } catch (WriterException ex) {
            throw new IOException("Unable to render QR code", ex);
        }
    }

    private List<String> buildSeatGroupLines(List<BookingSeat> seats) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (BookingSeat seat : seats) {
            var seatEntity = seat.getShowtimeSeat().getSeat();
            String label = seatEntity.getRowLabel() + seatEntity.getSeatNumber();
            String seatTypeName = seatEntity.getSeatType() != null ? seatEntity.getSeatType().getName() : "";
            String localized = SeatTypeLabelResolver.localize(seatTypeName);
            if (localized == null || localized.isBlank()) {
                localized = "Khac";
            }
            grouped.computeIfAbsent(localized, key -> new ArrayList<>()).add(label);
        }
        return grouped.entrySet().stream()
                .map(entry -> ascii(String.join(", ", entry.getValue()) + " - " + entry.getKey()))
                .toList();
    }

    private String ascii(String value) {
        return AsciiSanitizer.toAscii(value);
    }

    @lombok.Builder
    private record TicketPdfModel(
            String movieTitle,
            String originalTitle,
            String bookingCode,
            String cinemaName,
            String auditoriumName,
            String showtime,
            String issuedAt,
            String total,
            List<String> seatGroups,
            String qrDataUrl
    ) {
    }
}
