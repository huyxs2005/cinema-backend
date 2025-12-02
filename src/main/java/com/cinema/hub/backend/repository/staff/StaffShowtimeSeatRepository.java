package com.cinema.hub.backend.repository.staff;

import com.cinema.hub.backend.entity.ShowtimeSeat;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Integer> {

    @Query("""
        select new com.cinema.hub.backend.repository.staff.ShowtimeOccupancyView(
            ss.showtime.id,
            count(ss),
            sum(case when upper(ss.status) = 'AVAILABLE' then 1 else 0 end),
            sum(case when bs.id is not null then 1 else 0 end),
            sum(case when sh.id is not null then 1 else 0 end)
        )
        from ShowtimeSeat ss
        left join BookingSeat bs on bs.showtimeSeat = ss
            and bs.booking.bookingStatus <> com.cinema.hub.backend.entity.enums.BookingStatus.Cancelled
        left join SeatHold sh on sh.showtimeSeat = ss
            and sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
            and sh.expiresAt > :now
        where ss.showtime.id in :showtimeIds
        group by ss.showtime.id
    """)
    List<ShowtimeOccupancyView> calculateOccupancy(@Param("showtimeIds") List<Integer> showtimeIds,
                                                   @Param("now") OffsetDateTime now);
}
