package com.cinema.hub.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Showtimes",
        uniqueConstraints = @UniqueConstraint(name = "UQ_Showtimes", columnNames = {"AuditoriumId", "StartTime"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ShowtimeId")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MovieId", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AuditoriumId", nullable = false)
    private Auditorium auditorium;

    @Column(name = "StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "BasePrice", nullable = false, precision = 18, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "showtime", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<ShowtimeSeat> showtimeSeats = new LinkedHashSet<>();
}
