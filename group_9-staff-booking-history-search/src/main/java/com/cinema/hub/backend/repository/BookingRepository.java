package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Booking;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    Optional<Booking> findByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {
            "showtime",
            "showtime.movie",
            "showtime.auditorium",
            "bookingSeats",
            "bookingSeats.showtimeSeat",
            "bookingSeats.showtimeSeat.seat",
            "bookingSeats.showtimeSeat.seat.seatType",
            "bookingCombos",
            "bookingCombos.combo",
            "staff"
    })
    Optional<Booking> findDetailedByBookingCode(String bookingCode);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.auditorium", "staff"})
    List<Booking> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime from, OffsetDateTime to);

    @EntityGraph(attributePaths = {"showtime", "showtime.movie", "showtime.auditorium", "staff"})
    List<Booking> findTop20ByBookingCodeContainingIgnoreCaseOrCustomerPhoneContainingIgnoreCaseOrderByCreatedAtDesc(
            String bookingCode,
            String customerPhone);
}
