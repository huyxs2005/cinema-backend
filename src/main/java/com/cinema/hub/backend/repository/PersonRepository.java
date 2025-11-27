package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByFullNameIgnoreCase(String fullName);
}

