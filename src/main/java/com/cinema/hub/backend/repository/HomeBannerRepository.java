package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.HomeBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeBannerRepository extends JpaRepository<HomeBanner, Integer> {

    List<HomeBanner> findByIsActiveTrueOrderBySortOrderAsc();

    List<HomeBanner> findByMovieId(Integer movieId);
}
