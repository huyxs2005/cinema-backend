package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Integer> {

    Optional<Genre> findByNameIgnoreCase(String name);
}
