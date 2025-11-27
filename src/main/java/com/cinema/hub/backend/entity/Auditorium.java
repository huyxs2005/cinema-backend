package com.cinema.hub.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Auditoriums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditorium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditoriumId")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "NumberOfRows", nullable = false)
    @Builder.Default
    private Integer numberOfRows = 0;

    @Column(name = "NumberOfColumns", nullable = false)
    @Builder.Default
    private Integer numberOfColumns = 0;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "auditorium", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<Seat> seats = new LinkedHashSet<>();

    @OneToMany(mappedBy = "auditorium", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<Showtime> showtimes = new LinkedHashSet<>();
}
