package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Promotion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PromotionRepository extends JpaRepository<Promotion, Long>,
        JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}
