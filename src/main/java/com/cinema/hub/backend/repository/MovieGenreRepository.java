package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Movie;
import com.cinema.hub.backend.entity.MovieGenre;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.hub.backend.entity.MovieGenreId;

public interface MovieGenreRepository extends JpaRepository<MovieGenre, MovieGenreId> {

    void deleteByMovie(Movie movie);
}
