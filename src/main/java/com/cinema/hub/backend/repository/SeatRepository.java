package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Integer> {

    List<Seat> findByAuditorium_IdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(Integer auditoriumId);
}
