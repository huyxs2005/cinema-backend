package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.dto.staff.TicketScanResponse;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.Ticket;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.repository.staff.StaffTicketRepository;
import com.cinema.hub.backend.service.exception.StaffOperationException;
import com.cinema.hub.backend.util.TimeProvider;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StaffTicketService {

    private static final Pattern DIGIT_ONLY = Pattern.compile("^\\d+$");
    private static final DateTimeFormatter SHOWTIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final StaffTicketRepository staffTicketRepository;

    @Transactional(readOnly = true)
    public TicketScanResponse checkTicketByCode(String bookingCode) {
        List<Ticket> tickets = loadTicketsByBookingCode(bookingCode);
        return toResponse(tickets);
    }

    @Transactional(readOnly = true)
    public TicketScanResponse checkTicketManually(String value) {
        if (!StringUtils.hasText(value)) {
            throw new StaffOperationException("Giá trị tra cứu không hợp lệ");
        }
        String trimmed = value.trim();
        if (DIGIT_ONLY.matcher(trimmed).matches()) {
            int bookingId = Integer.parseInt(trimmed);
            List<Ticket> tickets = staffTicketRepository.findDetailedByBookingId(bookingId);
            if (tickets.isEmpty()) {
                throw new EntityNotFoundException("Không tìm thấy BookingId " + bookingId);
            }
            return toResponse(tickets);
        }
        return checkTicketByCode(trimmed);
    }

    @Transactional
    public TicketScanResponse checkInBooking(String bookingCode, UserAccount staffUser) {
        List<Ticket> tickets = loadTicketsByBookingCode(bookingCode);
        Booking booking = tickets.get(0).getBookingSeat().getBooking();
        ensureShowtimeNotEnded(booking);
        ensureBookingPaid(booking);
        OffsetDateTime now = TimeProvider.now();
        boolean updated = false;
        for (Ticket ticket : tickets) {
            if (ticket.getCheckedInAt() == null) {
                ticket.setCheckedInAt(now);
                ticket.setCheckedInByStaff(staffUser);
                updated = true;
            }
        }
        if (updated) {
            staffTicketRepository.saveAll(tickets);
        }
        return toResponse(tickets);
    }

    private List<Ticket> loadTicketsByBookingCode(String rawBookingCode) {
        if (!StringUtils.hasText(rawBookingCode)) {
            throw new StaffOperationException("Mã đặt vé không được để trống");
        }
        String normalized = rawBookingCode.trim();
        List<Ticket> tickets = staffTicketRepository.findDetailedByBookingCode(normalized);
        if (tickets.isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy vé với mã " + normalized);
        }
        return tickets;
    }

    private TicketScanResponse toResponse(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            throw new StaffOperationException("Không có vé nào để hiển thị");
        }
        Booking booking = tickets.get(0).getBookingSeat().getBooking();
        var showtime = booking.getShowtime();
        var user = booking.getUser();
        List<String> seats = tickets.stream()
                .map(ticket -> {
                    var seat = ticket.getBookingSeat().getShowtimeSeat().getSeat();
                    return seat.getRowLabel() + seat.getSeatNumber();
                })
                .sorted()
                .collect(Collectors.toList());
        long checkedCount = tickets.stream().filter(ticket -> ticket.getCheckedInAt() != null).count();
        int totalSeats = seats.size();
        boolean fullyCheckedIn = checkedCount == totalSeats && totalSeats > 0;
        String checkinState;
        if (fullyCheckedIn) {
            checkinState = "CHECKED_IN";
        } else if (checkedCount > 0) {
            checkinState = "PARTIAL";
        } else {
            checkinState = "PENDING";
        }
        boolean showtimeExpired = isShowtimeExpired(booking);
        boolean paid = booking.getPaymentStatus() == PaymentStatus.Paid;
        boolean checkinAllowed = paid && !fullyCheckedIn && !showtimeExpired;
        String showtimeLabel = SHOWTIME_FORMATTER.format(showtime.getStartTime())
                + " - "
                + SHOWTIME_FORMATTER.format(showtime.getEndTime());
        return TicketScanResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .movieTitle(showtime.getMovie().getTitle())
                .customerName(user != null ? user.getFullName() : null)
                .customerEmail(user != null ? user.getEmail() : null)
                .auditorium(showtime.getAuditorium().getName())
                .showtimeStart(showtime.getStartTime())
                .showtimeEnd(showtime.getEndTime())
                .showtimeLabel(showtimeLabel)
                .seats(seats)
                .paymentStatus(booking.getPaymentStatus())
                .paid(paid)
                .checkInStatus(checkinState)
                .checkedInCount((int) checkedCount)
                .totalSeats(totalSeats)
                .fullyCheckedIn(fullyCheckedIn)
                .checkinAllowed(checkinAllowed)
                .showtimeExpired(showtimeExpired)
                .build();
    }

    private boolean isShowtimeExpired(Booking booking) {
        OffsetDateTime end = booking.getShowtime().getEndTime().atOffset(TimeProvider.VN_ZONE_OFFSET);
        return end.isBefore(TimeProvider.now());
    }

    private void ensureShowtimeNotEnded(Booking booking) {
        if (isShowtimeExpired(booking)) {
            throw new StaffOperationException("Suất chiếu đã kết thúc, không thể check-in");
        }
    }

    private void ensureBookingPaid(Booking booking) {
        if (booking.getPaymentStatus() != PaymentStatus.Paid) {
            throw new StaffOperationException("Booking chưa thanh toán nên không thể check-in");
        }
    }
}
