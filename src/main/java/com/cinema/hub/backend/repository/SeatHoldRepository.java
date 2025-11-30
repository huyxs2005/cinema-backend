package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.SeatHold;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Integer> {

    void deleteByUser_Id(Integer userId);

    @Modifying
    @Query("delete from SeatHold sh where sh.status = :status and sh.expiresAt <= :cutoff")
    int deleteExpiredHolds(@Param("status") String status, @Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query("delete from SeatHold sh where sh.showtimeSeat.showtime.id = :showtimeId")
    int deleteByShowtimeId(@Param("showtimeId") Integer showtimeId);
}
