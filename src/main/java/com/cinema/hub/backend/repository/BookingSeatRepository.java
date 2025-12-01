package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Integer> {

    @Query("""
        select ss.id from BookingSeat bs
        join bs.booking b
        join bs.showtimeSeat ss
        where ss.id in :showtimeSeatIds
        and b.bookingStatus <> com.cinema.hub.backend.entity.enums.BookingStatus.Cancelled
    """)
    Set<Integer> findActiveSeatIds(@Param("showtimeSeatIds") Collection<Integer> showtimeSeatIds);

    @Query("""
        select bs from BookingSeat bs
        join fetch bs.showtimeSeat ss
        join fetch ss.seat seat
        where bs.booking.id = :bookingId
        order by seat.rowLabel asc, seat.seatNumber asc
    """)
    List<BookingSeat> findDetailedByBooking(@Param("bookingId") Integer bookingId);

    @Query("""
        select coalesce(sum(bs.finalPrice), 0) from BookingSeat bs
        where bs.booking.id = :bookingId
    """)
    java.math.BigDecimal calculateFinalAmountByBooking(@Param("bookingId") Integer bookingId);

    void deleteByBookingId(Integer bookingId);

    @Modifying
    @Query("""
        delete from BookingSeat bs
        where bs.showtimeSeat.id in :showtimeSeatIds
        and bs.booking.bookingStatus = com.cinema.hub.backend.entity.enums.BookingStatus.Cancelled
    """)
    int deleteCancelledSeatsByShowtimeSeatIds(@Param("showtimeSeatIds") Collection<Integer> showtimeSeatIds);
}
