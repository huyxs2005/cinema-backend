package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.config.DeadlockRetryable;
import com.cinema.hub.backend.dto.staff.StaffBookingQrDto;
import com.cinema.hub.backend.dto.staff.StaffBookingSeatDto;
import com.cinema.hub.backend.dto.staff.StaffBookingSummaryDto;
import com.cinema.hub.backend.dto.staff.WalkInBookingRequest;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.Ticket;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.payment.service.TicketEmailService;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.repository.PaymentLogRepository;
import com.cinema.hub.backend.repository.TicketRepository;
import com.cinema.hub.backend.repository.staff.StaffTicketRepository;
import com.cinema.hub.backend.entity.PaymentLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cinema.hub.backend.service.BookingService;
import com.cinema.hub.backend.service.exception.StaffOperationException;
import com.cinema.hub.backend.util.TimeProvider;
import com.cinema.hub.backend.util.PaymentMethodNormalizer;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffBookingService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TicketRepository ticketRepository;
    private final StaffTicketRepository staffTicketRepository;
    private final TicketEmailService ticketEmailService;
    private final PaymentLogRepository paymentLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public StaffBookingSummaryDto getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCodeWithShowtime(bookingCode)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingCode));
        // Don't include PDF in read-only transaction to avoid potential issues
        return buildSummary(booking, false);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<Map<String, Object>> getPaymentInfoForBooking(Integer bookingId) {
        try {
            List<PaymentLog> logs = paymentLogRepository.findByBooking_IdOrderByCreatedAtDesc(bookingId);
            if (logs.isEmpty()) {
                return Optional.empty();
            }
            PaymentLog latestLog = logs.get(0);
            if (latestLog.getRawMessage() == null) {
                return Optional.empty();
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> paymentInfo = (Map<String, Object>) objectMapper.readValue(latestLog.getRawMessage(), Map.class);
                paymentInfo.put("orderCode", latestLog.getProviderTransactionId());
                paymentInfo.put("amount", latestLog.getAmount());
                paymentInfo.put("status", latestLog.getStatus());
                return Optional.of(paymentInfo);
            } catch (Exception ex) {
                log.warn("Unable to parse payment log for booking {}: {}", bookingId, ex.getMessage());
                return Optional.empty();
            }
        } catch (Exception ex) {
            log.warn("Unable to retrieve payment log for booking {}: {}", bookingId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public StaffBookingQrDto getBookingQrInfo(String bookingCode) {
        StaffBookingSummaryDto booking = getBookingByCode(bookingCode);
        Optional<Map<String, Object>> paymentInfo = getPaymentInfoForBooking(booking.getBookingId());
        
        if (paymentInfo.isEmpty()) {
            throw new StaffOperationException("Chưa có thông tin VietQR cho đơn này");
        }
        
        Map<String, Object> info = paymentInfo.get();
        String qrBase64 = (String) info.get("qr");
        if (qrBase64 == null) {
            throw new StaffOperationException("Chưa có mã QR cho đơn này");
        }
        
        OffsetDateTime expiresAt = null;
        if (info.get("expiresAt") != null) {
            try {
                expiresAt = OffsetDateTime.parse(info.get("expiresAt").toString());
            } catch (Exception ex) {
                log.warn("Unable to parse expiresAt: {}", ex.getMessage());
            }
        }
        
        return StaffBookingQrDto.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .qrImageUrl("data:image/png;base64," + qrBase64)
                .amount((BigDecimal) info.get("amount"))
                .transferContent((String) info.get("transferContent"))
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StaffBookingSummaryDto> getBookingsByStatus(BookingStatus status) {
        List<Booking> bookings = bookingRepository.findByStatusWithShowtime(status);
        return bookings.stream()
                .map(booking -> buildSummary(booking, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StaffBookingSummaryDto> getBookingsByDate(java.time.OffsetDateTime date) {
        java.time.OffsetDateTime startOfDay = date.toLocalDate().atStartOfDay().atOffset(TimeProvider.VN_ZONE_OFFSET);
        java.time.OffsetDateTime endOfDay = startOfDay.plusDays(1);
        List<Booking> bookings = bookingRepository.findByDateWithShowtime(startOfDay, endOfDay);
        return bookings.stream()
                .map(booking -> buildSummary(booking, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StaffBookingSummaryDto> getPendingBookingsToday() {
        java.time.OffsetDateTime today = TimeProvider.now();
        java.time.OffsetDateTime startOfDay = today.toLocalDate().atStartOfDay().atOffset(TimeProvider.VN_ZONE_OFFSET);
        java.time.OffsetDateTime endOfDay = startOfDay.plusDays(1);
        List<BookingStatus> pendingStatuses = List.of(
                BookingStatus.Pending,
                BookingStatus.PendingVerification
        );
        List<Booking> bookings = bookingRepository.findByDateAndStatusesWithShowtime(startOfDay, endOfDay, pendingStatuses);
        return bookings.stream()
                .map(booking -> buildSummary(booking, false))
                .collect(Collectors.toList());
    }

    @DeadlockRetryable
    @Transactional
    public StaffBookingSummaryDto createWalkInBooking(WalkInBookingRequest request, UserAccount staffUser) {
        validateWalkInRequest(request);
        Booking booking = bookingService.createStaffBookingEntity(staffUser,
                request.getShowtimeId(),
                request.getSeatIds());
        booking.setCreatedByStaff(staffUser);
        booking.setCustomerEmail(normalizeEmail(request.getEmail()));
        booking.setCustomerPhone(normalizePhone(request.getPhone()));
        String normalizedMethod = normalizePaymentMethod(request.getPaymentMethod());
        booking.setPaymentMethod(normalizedMethod);
        applyInitialPaymentState(booking, normalizedMethod);
        booking = saveBookingWithStatusFallback(booking);
        applyDiscount(booking, request);
        
        Booking reloaded = bookingRepository.findByBookingCodeWithShowtime(booking.getBookingCode())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found after creation"));
        return buildSummary(reloaded, false);
    }

    @DeadlockRetryable
    @Transactional
    public StaffBookingSummaryDto verifyBooking(Integer bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getBookingStatus() != BookingStatus.Pending
                && booking.getBookingStatus() != BookingStatus.PendingVerification) {
            throw new StaffOperationException("Booking is not pending verification");
        }
        if (booking.getPaymentMethod() == null) {
            throw new StaffOperationException("Hình thức thanh toán chưa được chọn");
        }
        Booking updated = bookingService.markBookingPaid(booking, booking.getPaymentMethod());
        byte[] pdfBytes = null;
        try {
            pdfBytes = ticketEmailService.generatePdf(updated.getId());
        } catch (RuntimeException ex) {
            log.warn("Unable to generate ticket PDF for booking {}: {}", updated.getBookingCode(), ex.getMessage());
        }
        String email = updated.getCustomerEmail();
        if (StringUtils.hasText(email)) {
            try {
                ticketEmailService.sendTicket(updated.getId(), email);
            } catch (RuntimeException ex) {
                log.warn("Unable to send ticket email for booking {}: {}", updated.getBookingCode(), ex.getMessage());
            }
        }
        StaffBookingSummaryDto summary = buildSummary(updated, false);
        if (pdfBytes != null && pdfBytes.length > 0) {
            summary.setTicketPdfBase64(Base64.getEncoder().encodeToString(pdfBytes));
        }
        return summary;
    }

    @DeadlockRetryable
    @Transactional
    public StaffBookingSummaryDto cancelBooking(Integer bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getBookingStatus() != BookingStatus.Pending
                && booking.getBookingStatus() != BookingStatus.PendingVerification) {
            throw new StaffOperationException("Booking cannot be cancelled in its current state");
        }
        booking.setBookingStatus(BookingStatus.Cancelled);
        booking.setPaymentStatus(PaymentStatus.Unpaid);
        booking.setCancelledAt(TimeProvider.now());
        bookingRepository.save(booking);
        ticketRepository.deleteByBookingSeat_Booking_Id(bookingId);
        bookingSeatRepository.deleteByBookingId(bookingId);
        return buildSummary(booking, false);
    }

    @DeadlockRetryable
    @Transactional
    public StaffBookingSummaryDto updatePaymentMethod(Integer bookingId, String rawMethod) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getPaymentStatus() == PaymentStatus.Paid) {
            throw new StaffOperationException("Payment method cannot be changed after payment");
        }
        booking.setPaymentMethod(normalizePaymentMethod(rawMethod));
        bookingRepository.save(booking);
        return buildSummary(booking, false);
    }

    private void validateWalkInRequest(WalkInBookingRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new StaffOperationException("Seat list cannot be empty");
        }
        if (request.getFinalPrice() == null || request.getFinalPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new StaffOperationException("Invalid final price");
        }
    }

    private void applyDiscount(Booking booking, WalkInBookingRequest request) {
        BigDecimal percent = request.getDiscountPercent() != null
                ? request.getDiscountPercent()
                : BigDecimal.ZERO;
        if (percent.compareTo(BigDecimal.ZERO) < 0 || percent.compareTo(BigDecimal.ONE) > 0) {
            throw new StaffOperationException("Invalid discount percent");
        }
        List<BookingSeat> seats = bookingSeatRepository.findDetailedByBooking(booking.getId());
        if (seats.isEmpty()) {
            throw new StaffOperationException("No seats associated with booking");
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(percent);
        BigDecimal recalculated = BigDecimal.ZERO;
        for (BookingSeat seat : seats) {
            BigDecimal base = seat.getUnitPrice();
            BigDecimal discounted = base.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
            if (discounted.compareTo(BigDecimal.ZERO) < 0) {
                discounted = BigDecimal.ZERO;
            }
            BigDecimal discountAmount = base.subtract(discounted);
            if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
                discountAmount = BigDecimal.ZERO;
            }
            seat.setDiscountAmount(discountAmount);
            seat.setFinalPrice(discounted);
            recalculated = recalculated.add(discounted);
        }
        bookingSeatRepository.saveAll(seats);
        booking.setFinalAmount(recalculated);
        saveBookingWithStatusFallback(booking);
        BigDecimal provided = request.getFinalPrice();
        if (provided != null) {
            BigDecimal difference = recalculated.subtract(provided).abs();
            if (difference.compareTo(new BigDecimal("1000")) > 0) {
                throw new StaffOperationException("Final payment does not match calculated amount");
            }
        }
    }

    private void applyInitialPaymentState(Booking booking, String paymentMethod) {
        if ("Cash".equalsIgnoreCase(paymentMethod)) {
            booking.setBookingStatus(BookingStatus.Confirmed);
            booking.setPaymentStatus(PaymentStatus.Paid);
            booking.setPaidAt(TimeProvider.now());
            return;
        }
        booking.setPaidAt(null);
        if (StringUtils.hasText(paymentMethod)) {
            booking.setBookingStatus(BookingStatus.PendingVerification);
            booking.setPaymentStatus(PaymentStatus.Unpaid);
        } else {
            booking.setBookingStatus(BookingStatus.Pending);
            booking.setPaymentStatus(PaymentStatus.Unpaid);
        }
    }

    private StaffBookingSummaryDto buildSummary(Booking booking, boolean includePdf) {
        List<BookingSeat> seats = bookingSeatRepository.findDetailedByBooking(booking.getId());
        Map<Integer, Ticket> ticketMap = staffTicketRepository.findDetailedByBookingId(booking.getId())
                .stream()
                .collect(Collectors.toMap(ticket -> ticket.getBookingSeat().getId(), Function.identity()));
        List<StaffBookingSeatDto> seatDtos = seats.stream()
                .map(seat -> {
                    Ticket ticket = ticketMap.get(seat.getId());
                    String label = seat.getShowtimeSeat().getSeat().getRowLabel()
                            + seat.getShowtimeSeat().getSeat().getSeatNumber();
                    return StaffBookingSeatDto.builder()
                            .seatId(seat.getShowtimeSeat().getSeat().getId())
                            .seatLabel(label)
                            .seatType(seat.getShowtimeSeat().getSeat().getSeatType().getName())
                            .finalPrice(seat.getFinalPrice())
                            .checkedIn(ticket != null && ticket.getCheckedInAt() != null)
                            .checkedInAt(ticket != null ? ticket.getCheckedInAt() : null)
                            .build();
                })
                .collect(Collectors.toList());
        String pdfBase64 = null;
        if (includePdf && booking.getPaymentStatus() == PaymentStatus.Paid) {
            try {
                byte[] pdfBytes = ticketEmailService.generatePdf(booking.getId());
                if (pdfBytes != null && pdfBytes.length > 0) {
                    pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
                }
            } catch (Exception ex) {
                log.warn("Unable to render ticket PDF for booking {}: {}", booking.getBookingCode(), ex.getMessage());
            }
        }
        UserAccount user = booking.getUser();
        String resolvedEmail = booking.getCustomerEmail() != null
                ? booking.getCustomerEmail()
                : (user != null ? user.getEmail() : null);
        String resolvedPhone = booking.getCustomerPhone() != null
                ? booking.getCustomerPhone()
                : (user != null ? user.getPhone() : null);
        return StaffBookingSummaryDto.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .auditoriumName(booking.getShowtime().getAuditorium().getName())
                .showtimeStart(booking.getShowtime().getStartTime())
                .showtimeEnd(booking.getShowtime().getEndTime())
                .customerEmail(resolvedEmail)
                .customerPhone(resolvedPhone)
                .createdByStaffName(booking.getCreatedByStaff() != null
                        ? booking.getCreatedByStaff().getFullName()
                        : null)
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .paymentMethod(booking.getPaymentMethod())
                .totalAmount(booking.getTotalAmount())
                .finalAmount(booking.getFinalAmount())
                .paidAt(booking.getPaidAt())
                .createdAt(booking.getCreatedAt())
                .seats(seatDtos)
                .ticketPdfBase64(pdfBase64)
                .build();
    }

    private Booking saveBookingWithStatusFallback(Booking booking) {
        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            if (isBookingStatusConstraintViolation(ex)) {
                BookingStatus fallback = fallbackStatus(booking.getBookingStatus());
                if (fallback != booking.getBookingStatus()) {
                    log.warn("Booking {} status {} is not supported by current schema; falling back to {}",
                            booking.getBookingCode(), booking.getBookingStatus(), fallback);
                    booking.setBookingStatus(fallback);
                    return bookingRepository.save(booking);
                }
            }
            throw ex;
        }
    }

    private BookingStatus fallbackStatus(BookingStatus current) {
        if (current == BookingStatus.PendingVerification) {
            return BookingStatus.Pending;
        }
        if (current == BookingStatus.Refunded) {
            return BookingStatus.Cancelled;
        }
        return current;
    }

    private boolean isBookingStatusConstraintViolation(Throwable ex) {
        while (ex != null) {
            String message = ex.getMessage();
            if (message != null && message.contains("CK_Bookings_Status")) {
                return true;
            }
            ex = ex.getCause();
        }
        return false;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digitsOnly = phone.replaceAll("\\D+", "");
        return digitsOnly.isEmpty() ? null : digitsOnly;
    }

    private String normalizePaymentMethod(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return PaymentMethodNormalizer.normalize(raw);
        } catch (IllegalArgumentException ex) {
            throw new StaffOperationException(ex.getMessage());
        }
    }
}
