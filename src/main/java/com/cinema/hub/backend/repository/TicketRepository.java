package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    void deleteByBookingSeat_Booking_Id(Integer bookingId);

    @Modifying
    @Query("""
        delete from Ticket t
        where t.bookingSeat.id in (
            select bs.id from BookingSeat bs
            where bs.showtimeSeat.id in :showtimeSeatIds
            and bs.booking.bookingStatus = com.cinema.hub.backend.entity.enums.BookingStatus.Cancelled
        )
    """)
    int deleteCancelledTicketsByShowtimeSeatIds(@Param("showtimeSeatIds") Collection<Integer> showtimeSeatIds);
}
