package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.SeatHold;
import com.cinema.hub.backend.entity.UserAccount;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Integer> {

    @Query("""
            select sh.showtimeSeat.id
            from SeatHold sh
            where sh.showtimeSeat.showtime.id = :showtimeId
              and sh.status = 'Held'
              and sh.expiresAt > :currentTime
            """)
    Set<Integer> findActiveHeldSeatIds(@Param("showtimeId") Integer showtimeId,
                                       @Param("currentTime") OffsetDateTime currentTime);

    @Query("""
            select sh.showtimeSeat.id
            from SeatHold sh
            where sh.showtimeSeat.showtime.id = :showtimeId
              and sh.status = 'Held'
              and sh.expiresAt > :currentTime
              and sh.user.id = :userId
            """)
    Set<Integer> findActiveHeldSeatIdsForUser(@Param("showtimeId") Integer showtimeId,
                                              @Param("userId") Integer userId,
                                              @Param("currentTime") OffsetDateTime currentTime);

    Optional<SeatHold> findTop1ByShowtimeSeat_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Integer showtimeSeatId, String status, OffsetDateTime currentTime);

    List<SeatHold> findByShowtimeSeat_IdInAndStatusAndUser(Collection<Integer> showtimeSeatIds,
                                                          String status,
                                                          UserAccount user);

    List<SeatHold> findByStatusAndExpiresAtBefore(String status, OffsetDateTime cutoffTime);
}
