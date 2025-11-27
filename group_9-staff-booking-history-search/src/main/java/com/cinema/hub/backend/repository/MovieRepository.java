package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Integer> {

    @Query("""
            select distinct m from Movie m
            left join fetch m.movieGenres mg
            left join fetch mg.genre
            where m.status = :status
            """)
    List<Movie> findAllByStatusWithGenres(@Param("status") String status);

    @Query("""
            select distinct m from Movie m
            left join fetch m.movieGenres mg
            left join fetch mg.genre
            """)
    List<Movie> findAllWithGenres();

    @Query("""
            select distinct m from Movie m
            left join fetch m.movieGenres mg
            left join fetch mg.genre
            left join fetch m.credits c
            left join fetch c.person
            where m.id = :movieId
            """)
    Optional<Movie> findMovieDetailById(@Param("movieId") Integer movieId);

    List<Movie> findByStatusInOrderByTitleAsc(List<String> statuses);
}
