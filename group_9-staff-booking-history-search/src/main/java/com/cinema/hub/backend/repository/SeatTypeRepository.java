package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.SeatType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatTypeRepository extends JpaRepository<SeatType, Integer> {

    Optional<SeatType> findByNameIgnoreCase(String name);

    Optional<SeatType> findFirstByActiveTrueOrderByIdAsc();
}
