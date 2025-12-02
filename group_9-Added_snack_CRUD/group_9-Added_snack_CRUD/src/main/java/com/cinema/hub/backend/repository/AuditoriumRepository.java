package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Auditorium;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Integer>, JpaSpecificationExecutor<Auditorium> {

    List<Auditorium> findByActiveTrueOrderByNameAsc();
}
