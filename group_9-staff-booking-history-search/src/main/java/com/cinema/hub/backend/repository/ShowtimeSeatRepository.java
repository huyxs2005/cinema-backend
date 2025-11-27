package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.ShowtimeSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Integer> {

    List<ShowtimeSeat> findByShowtime_IdOrderBySeat_RowLabelAscSeat_SeatNumberAsc(Integer showtimeId);

    long countByShowtime_Id(Integer showtimeId);
}
