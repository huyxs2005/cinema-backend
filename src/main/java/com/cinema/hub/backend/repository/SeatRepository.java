package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Integer> {

    List<Seat> findByAuditorium_IdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Integer auditoriumId);

    Optional<Seat> findByAuditorium_IdAndRowLabelIgnoreCaseAndSeatNumber(Integer auditoriumId, String rowLabel, Integer seatNumber);

    void deleteByAuditorium_Id(Integer auditoriumId);

    long countByAuditorium_Id(Integer auditoriumId);

    @Query("""
        select s.seatType.id, count(distinct s.rowLabel)
        from Seat s
        where s.auditorium.id = :auditoriumId
        group by s.seatType.id
    """)
    List<Object[]> countDistinctRowsBySeatType(@Param("auditoriumId") Integer auditoriumId);
}
