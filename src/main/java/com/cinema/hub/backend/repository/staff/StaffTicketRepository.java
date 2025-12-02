package com.cinema.hub.backend.repository.staff;

import com.cinema.hub.backend.entity.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffTicketRepository extends JpaRepository<Ticket, Integer> {

    @Query("""
        select t from Ticket t
        join fetch t.bookingSeat bs
        join fetch bs.showtimeSeat ss
        join fetch ss.seat seat
        join fetch seat.seatType st
        join fetch ss.showtime showtime
        join fetch showtime.movie movie
        join fetch showtime.auditorium auditorium
        join fetch bs.booking booking
        left join fetch booking.user user
        left join fetch t.checkedInByStaff staff
        where booking.id = :bookingId
        order by seat.rowLabel asc, seat.seatNumber asc
    """)
    List<Ticket> findDetailedByBookingId(@Param("bookingId") Integer bookingId);

    @Query("""
        select t from Ticket t
        join fetch t.bookingSeat bs
        join fetch bs.showtimeSeat ss
        join fetch ss.seat seat
        join fetch seat.seatType st
        join fetch ss.showtime showtime
        join fetch showtime.movie movie
        join fetch showtime.auditorium auditorium
        join fetch bs.booking booking
        left join fetch booking.user user
        left join fetch t.checkedInByStaff staff
        where booking.bookingCode = :bookingCode
        order by seat.rowLabel asc, seat.seatNumber asc
    """)
    List<Ticket> findDetailedByBookingCode(@Param("bookingCode") String bookingCode);
}
