package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.dto.staff.StaffBookingSeatDto;
import com.cinema.hub.backend.dto.staff.StaffBookingSummaryDto;
import com.cinema.hub.backend.dto.staff.WalkInBookingRequest;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.Ticket;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.payment.service.PaymentService;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.repository.staff.StaffTicketRepository;
import com.cinema.hub.backend.service.BookingService;
import com.cinema.hub.backend.service.exception.StaffOperationException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffBookingService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final StaffTicketRepository staffTicketRepository;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public StaffBookingSummaryDto getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCodeWithShowtime(bookingCode)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingCode));
        return buildSummary(booking);
    }

    @Transactional
    public StaffBookingSummaryDto createWalkInBooking(WalkInBookingRequest request, UserAccount staffUser) {
        validateWalkInRequest(request);
        String paymentLabel = resolvePaymentMethodLabel(request.getPaymentMethod());
        Booking booking = bookingService.createBookingEntity(staffUser,
                request.getShowtimeId(),
                request.getSeatIds(),
                paymentLabel);
        booking.setCreatedByStaff(staffUser);
        applyDiscount(booking, request);
        booking = bookingService.markBookingPaid(booking, paymentLabel);
        Booking reloaded = bookingRepository.findByBookingCodeWithShowtime(booking.getBookingCode())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found after creation"));
        return buildSummary(reloaded, request.getFullName(), request.getEmail(), request.getPhone());
    }

    private void validateWalkInRequest(WalkInBookingRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new StaffOperationException("Seat list cannot be empty");
        }
        if (request.getFinalPrice() == null || request.getFinalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new StaffOperationException("Invalid final price");
        }
        String method = request.getPaymentMethod();
        if (!"CASH".equalsIgnoreCase(method) && !"TRANSFER".equalsIgnoreCase(method)) {
            throw new StaffOperationException("Unsupported payment method");
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
        bookingRepository.save(booking);
        BigDecimal provided = request.getFinalPrice();
        if (provided != null) {
            BigDecimal difference = recalculated.subtract(provided).abs();
            if (difference.compareTo(new BigDecimal("1000")) > 0) {
                throw new StaffOperationException("Final payment does not match calculated amount");
            }
        }
    }

    private String resolvePaymentMethodLabel(String raw) {
        if ("TRANSFER".equalsIgnoreCase(raw)) {
            return "Bank Transfer";
        }
        return "Cash";
    }

    private StaffBookingSummaryDto buildSummary(Booking booking) {
        return buildSummary(booking,
                booking.getUser() != null ? booking.getUser().getFullName() : null,
                booking.getUser() != null ? booking.getUser().getEmail() : null,
                booking.getUser() != null ? booking.getUser().getPhone() : null);
    }

    private StaffBookingSummaryDto buildSummary(Booking booking,
                                                String customerNameOverride,
                                                String customerEmailOverride,
                                                String customerPhoneOverride) {
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
        if (booking.getPaymentStatus() == PaymentStatus.Paid) {
            byte[] pdfBytes = paymentService.generateTicketPdf(booking.getId());
            pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);
        }
        UserAccount user = booking.getUser();
        String resolvedName = customerNameOverride != null ? customerNameOverride : (user != null ? user.getFullName() : null);
        String resolvedEmail = customerEmailOverride != null ? customerEmailOverride : (user != null ? user.getEmail() : null);
        String resolvedPhone = customerPhoneOverride != null ? customerPhoneOverride : (user != null ? user.getPhone() : null);
        return StaffBookingSummaryDto.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .auditoriumName(booking.getShowtime().getAuditorium().getName())
                .showtimeStart(booking.getShowtime().getStartTime())
                .showtimeEnd(booking.getShowtime().getEndTime())
                .customerName(resolvedName)
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
                .seats(seatDtos)
                .ticketPdfBase64(pdfBase64)
                .build();
    }
}
