package com.cinema.hub.backend.service.impl.staff;

import com.cinema.hub.backend.dto.staff.dashboard.StaffBookingSummaryDto;
import com.cinema.hub.backend.dto.staff.dashboard.StaffShowtimeSummaryDto;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.Showtime;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.repository.SeatHoldRepository;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.repository.ShowtimeSeatRepository;
import com.cinema.hub.backend.service.staff.StaffDashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffDashboardServiceImpl implements StaffDashboardService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingRepository bookingRepository;

    @Override
    public List<StaffShowtimeSummaryDto> getUpcomingShowtimes(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(daysAhead).atTime(23, 59, 59);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return showtimeRepository.findByStartTimeBetween(from, to).stream()
                .map(showtime -> toShowtimeSummary(showtime, now))
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffBookingSummaryDto> getTodayBookings() {
        LocalDate today = LocalDate.now();
        OffsetDateTime from = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return bookingRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(this::toBookingSummary)
                .toList();
    }

    @Override
    public List<StaffBookingSummaryDto> searchBookings(String keyword) {
        String sanitized = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (sanitized.isEmpty()) {
            return List.of();
        }
        return bookingRepository
                .findTop20ByBookingCodeContainingIgnoreCaseOrCustomerPhoneContainingIgnoreCaseOrderByCreatedAtDesc(
                        sanitized,
                        sanitized)
                .stream()
                .map(this::toBookingSummary)
                .toList();
    }

    private StaffShowtimeSummaryDto toShowtimeSummary(Showtime showtime, OffsetDateTime now) {
        long totalSeats = showtimeSeatRepository.countByShowtime_Id(showtime.getId());
        Set<Integer> soldSeatIds = bookingSeatRepository.findLockedSeatIdsForShowtime(showtime.getId());
        Set<Integer> heldSeatIds = seatHoldRepository.findActiveHeldSeatIds(showtime.getId(), now);
        int heldSeats = (int) heldSeatIds.stream()
                .filter(id -> id != null && !soldSeatIds.contains(id))
                .count();

        int soldSeats = soldSeatIds.size();
        int available = (int) Math.max(0, totalSeats - soldSeats - heldSeats);
        return StaffShowtimeSummaryDto.builder()
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .auditoriumName(showtime.getAuditorium().getName())
                .startTime(showtime.getStartTime())
                .totalSeats((int) totalSeats)
                .soldSeats((int) soldSeats)
                .heldSeats(heldSeats)
                .availableSeats(available)
                .build();
    }

    private StaffBookingSummaryDto toBookingSummary(Booking booking) {
        return StaffBookingSummaryDto.builder()
                .bookingCode(booking.getBookingCode())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .auditoriumName(booking.getShowtime().getAuditorium().getName())
                .showtimeStart(booking.getShowtime().getStartTime())
                .paymentMethod(booking.getPaymentMethod())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .finalAmount(defaultAmount(booking.getFinalAmount()))
                .customerPhone(booking.getCustomerPhone())
                .createdAt(booking.getCreatedAt())
                .staffName(booking.getStaff() != null ? booking.getStaff().getFullName() : null)
                .build();
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }
}
