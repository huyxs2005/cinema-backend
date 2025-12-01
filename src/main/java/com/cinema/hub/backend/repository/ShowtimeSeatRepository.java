package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.dto.SeatStatusRow;
import com.cinema.hub.backend.entity.ShowtimeSeat;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Integer> {

    void deleteByShowtime_Id(Integer showtimeId);

    @Query("""
        select new com.cinema.hub.backend.dto.SeatStatusRow(
            seat.id,
            seat.rowLabel,
            seat.seatNumber,
            seat.seatType.name,
            null,
            ss.effectivePrice,
            ss.status,
            sh.status,
            sh.expiresAt,
            b.bookingStatus,
            b.paymentStatus,
            sh.user.id
        )
        from ShowtimeSeat ss
        join ss.seat seat
        left join SeatHold sh on sh.showtimeSeat = ss
            and sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
            and sh.expiresAt > :now
        left join BookingSeat bs on bs.showtimeSeat = ss
        left join bs.booking b on b.bookingStatus <> com.cinema.hub.backend.entity.enums.BookingStatus.Cancelled
        where ss.showtime.id = :showtimeId
        order by seat.rowLabel asc, seat.seatNumber asc
    """)
    List<SeatStatusRow> fetchSeatStatusRows(@Param("showtimeId") int showtimeId, @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select ss from ShowtimeSeat ss
        join fetch ss.seat seat
        join fetch ss.showtime showtime
        where showtime.id = :showtimeId
          and seat.id in :seatIds
    """)
    List<ShowtimeSeat> lockSeatsForHold(@Param("showtimeId") int showtimeId,
                                        @Param("seatIds") Collection<Integer> seatIds);
}
