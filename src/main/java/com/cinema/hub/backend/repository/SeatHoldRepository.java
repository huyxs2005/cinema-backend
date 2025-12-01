package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.SeatHold;
import com.cinema.hub.backend.entity.enums.SeatHoldStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Integer> {

    void deleteByUser_Id(Integer userId);

    @Modifying
    @Query("delete from SeatHold sh where sh.showtimeSeat.showtime.id = :showtimeId")
    int deleteByShowtimeId(@Param("showtimeId") Integer showtimeId);

    @Query("""
        select sh from SeatHold sh
        where sh.showtimeSeat.id in :showtimeSeatIds
          and sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
          and sh.expiresAt > :now
    """)
    List<SeatHold> findActiveHoldsBySeatIds(@Param("showtimeSeatIds") Collection<Integer> showtimeSeatIds,
                                            @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select sh from SeatHold sh
        join fetch sh.showtimeSeat ss
        join fetch ss.showtime showtime
        join fetch ss.seat seat
        where sh.holdToken = :token
          and sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
          and sh.expiresAt > :now
    """)
    List<SeatHold> findActiveHoldsByTokenForUpdate(@Param("token") UUID token,
                                                   @Param("now") OffsetDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("""
        update SeatHold sh
        set sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Expired
        where sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
          and sh.expiresAt < :now
    """)
    int expireStaleHolds(@Param("now") OffsetDateTime now);

    default int deleteExpiredHolds(SeatHoldStatus status, OffsetDateTime cutoff) {
        return expireStaleHolds(cutoff);
    }

    @Modifying(clearAutomatically = true)
    @Query("""
        update SeatHold sh
        set sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Released
        where sh.holdToken = :token
          and sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
    """)
    int releaseByToken(@Param("token") java.util.UUID token);

    @Modifying(clearAutomatically = true)
    @Query("""
        update SeatHold sh
        set sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Released
        where sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
          and sh.user.id = :userId
    """)
    int releaseByUserId(@Param("userId") Integer userId);

    @Query("""
        select sh from SeatHold sh
        join fetch sh.showtimeSeat ss
        join fetch ss.seat seat
        join fetch ss.showtime showtime
        join fetch showtime.movie movie
        join fetch showtime.auditorium auditorium
        where sh.holdToken = :token
          and sh.status = com.cinema.hub.backend.entity.enums.SeatHoldStatus.Held
          and sh.expiresAt > :now
    """)
    List<SeatHold> findActiveHoldsByToken(@Param("token") UUID token,
                                          @Param("now") OffsetDateTime now);
}
