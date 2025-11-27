package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Combo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboRepository extends JpaRepository<Combo, Integer> {

    List<Combo> findByActiveTrueOrderByPriceAsc();
}
