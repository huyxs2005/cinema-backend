package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Showtime;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Integer>, JpaSpecificationExecutor<Showtime> {

    @Query("""
            select case when count(s) > 0 then true else false end
            from Showtime s
            where s.auditorium.id = :auditoriumId
              and s.active = true
              and (:excludeId is null or s.id <> :excludeId)
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    boolean existsConflictingShowtime(@Param("auditoriumId") Integer auditoriumId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("excludeId") Integer excludeId);

    List<Showtime> findByMovie_IdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(Integer movieId,
                                                                                     LocalDateTime start,
                                                                                     LocalDateTime end);

    List<Showtime> findByActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(LocalDateTime start,
                                                                          LocalDateTime end);

    boolean existsByAuditorium_Id(Integer auditoriumId);
}
