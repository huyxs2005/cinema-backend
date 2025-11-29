package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Integer> {

    void deleteByUser_Id(Integer userId);
}

