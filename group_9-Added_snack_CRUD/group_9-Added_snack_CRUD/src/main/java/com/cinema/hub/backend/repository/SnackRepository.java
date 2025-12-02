package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Snack;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SnackRepository extends JpaRepository<Snack, Long>, JpaSpecificationExecutor<Snack> {

    Optional<Snack> findBySlugIgnoreCase(String slug);
}
