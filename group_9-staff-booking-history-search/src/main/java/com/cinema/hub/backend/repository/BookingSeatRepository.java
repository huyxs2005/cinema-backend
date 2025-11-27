package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.BookingSeat;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Integer> {

    @Query("""
            select bs.showtimeSeat.id
            from BookingSeat bs
            where bs.showtimeSeat.showtime.id = :showtimeId
              and bs.booking.bookingStatus <> 'Cancelled'
            """)
    Set<Integer> findLockedSeatIdsForShowtime(@Param("showtimeId") Integer showtimeId);

    @Query("""
            select count(bs.id)
            from BookingSeat bs
            where bs.showtimeSeat.id in :seatIds
              and bs.booking.bookingStatus <> 'Cancelled'
            """)
    long countActiveSeatsForShowtimeSeats(@Param("seatIds") Collection<Integer> seatIds);
}
