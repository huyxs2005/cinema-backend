package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.CreateBookingRequest;
import com.cinema.hub.backend.dto.CreateBookingResponse;
import com.cinema.hub.backend.dto.SeatHoldRequest;
import com.cinema.hub.backend.dto.SeatHoldResponse;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.web.view.BookingConfirmationView;
import com.cinema.hub.backend.web.view.BookingHistoryView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.cinema.hub.backend.util.TimeProvider;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final SeatReservationService seatReservationService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Transactional
    public BookingConfirmationView createBooking(UserAccount user,
                                                 int showtimeId,
                                                 List<Integer> seatIds,
                                                 String paymentMethod) {
        Booking booking = createBookingEntity(user, showtimeId, seatIds, paymentMethod);
        return buildConfirmationView(booking);
    }

    @Transactional
    public Booking createBookingEntity(UserAccount user,
                                       int showtimeId,
                                       List<Integer> seatIds,
                                       String paymentMethod) {
        return createBookingInternal(user, showtimeId, seatIds, paymentMethod);
    }

    @Transactional
    public Booking createBookingFromHold(UserAccount user, String holdToken, String paymentMethod) {
        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setHoldToken(holdToken);
        bookingRequest.setUserId(user.getId());
        bookingRequest.setPaymentMethod(paymentMethod);
        CreateBookingResponse bookingResponse = seatReservationService.createBooking(bookingRequest);
        return bookingRepository.findById(bookingResponse.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found after creation"));
    }

    private Booking createBookingInternal(UserAccount user,
                                          int showtimeId,
                                          List<Integer> seatIds,
                                          String paymentMethod) {
        SeatHoldRequest holdRequest = new SeatHoldRequest();
        holdRequest.setShowtimeId(showtimeId);
        holdRequest.setSeatIds(seatIds);
        holdRequest.setUserId(user.getId());
        SeatHoldResponse holdResponse = seatReservationService.holdSeats(holdRequest);

        CreateBookingRequest bookingRequest = new CreateBookingRequest();
        bookingRequest.setHoldToken(holdResponse.getHoldToken());
        bookingRequest.setUserId(user.getId());
        bookingRequest.setPaymentMethod(paymentMethod);

        CreateBookingResponse bookingResponse = seatReservationService.createBooking(bookingRequest);
        Booking booking = bookingRepository.findById(bookingResponse.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found after creation"));
        return booking;
    }

    @Transactional(readOnly = true)
    public BookingConfirmationView getConfirmationView(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCodeWithShowtime(bookingCode)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingCode));
        return buildConfirmationView(booking);
    }

    @Transactional(readOnly = true)
    public BookingConfirmationView getConfirmationViewForUser(String bookingCode, UserAccount user) {
        if (user == null) {
            throw new AccessDeniedException("User context is required.");
        }
        Booking booking = bookingRepository.findByBookingCodeAndUserWithShowtime(bookingCode, user)
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this booking."));
        return buildConfirmationView(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryView> getBookingHistory(UserAccount user) {
        return bookingRepository.findTop10ByUserAndBookingStatusOrderByCreatedAtDesc(user, BookingStatus.Confirmed)
                .stream()
                .map(booking -> {
                    List<String> seatLabels = loadSeatLabels(booking);
                    if (seatLabels.isEmpty()) {
                        return null;
                    }
                    BigDecimal totalAmount = booking.getFinalAmount() != null && booking.getFinalAmount().signum() > 0
                            ? booking.getFinalAmount()
                            : booking.getTotalAmount();
                    return BookingHistoryView.builder()
                            .bookingCode(booking.getBookingCode())
                            .movieTitle(booking.getShowtime().getMovie().getTitle())
                            .format(resolveBookingFormat(booking))
                            .theaterName(booking.getShowtime().getAuditorium().getName())
                            .showtime(booking.getShowtime().getStartTime())
                            .seatLabels(String.join(", ", seatLabels))
                            .ticketCount(seatLabels.size())
                            .total(totalAmount)
                            .purchasedAt(booking.getPaidAt() != null ? booking.getPaidAt() : booking.getCreatedAt())
                            .status(booking.getBookingStatus())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }


    private BookingConfirmationView buildConfirmationView(Booking booking) {
        List<String> seatLabels = loadSeatLabels(booking);
        BigDecimal total = booking.getFinalAmount() != null ? booking.getFinalAmount() : booking.getTotalAmount();
        String showtimeText = booking.getShowtime().getStartTime()
                .format(DateTimeFormatter.ofPattern("EEE, MMM d - h:mm a"));
        return BookingConfirmationView.builder()
                .bookingCode(booking.getBookingCode())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .theaterName(booking.getShowtime().getAuditorium().getName())
                .auditoriumName(booking.getShowtime().getAuditorium().getName())
                .format(resolveBookingFormat(booking))
                .email(booking.getUser() != null ? booking.getUser().getEmail() : "")
                .seatLabels(seatLabels)
                .showtimeText(showtimeText)
                .total(total)
                .build();
    }

    @Transactional
    public Booking markBookingPaid(Booking booking, String paymentMethod) {
        if (booking.getPaymentStatus() == PaymentStatus.Paid) {
            return booking;
        }
        booking.setPaymentStatus(PaymentStatus.Paid);
        booking.setBookingStatus(BookingStatus.Confirmed);
        booking.setPaymentMethod(paymentMethod);
        booking.setPaidAt(TimeProvider.now());
        return bookingRepository.save(booking);
    }

    private String resolveBookingFormat(Booking booking) {
        return "2D";
    }

    private List<String> loadSeatLabels(Booking booking) {
        List<BookingSeat> seats = bookingSeatRepository.findDetailedByBooking(booking.getId());
        return seats.stream()
                .map(bs -> {
                    var seat = bs.getShowtimeSeat().getSeat();
                    return seat.getRowLabel() + seat.getSeatNumber();
                })
                .collect(Collectors.toList());
    }
}

