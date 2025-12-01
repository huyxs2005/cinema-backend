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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Seats",
        uniqueConstraints = @UniqueConstraint(name = "UQ_Seats", columnNames = {"AuditoriumId", "RowLabel", "SeatNumber"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SeatId")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AuditoriumId", nullable = false)
    private Auditorium auditorium;

    @Column(name = "RowLabel", nullable = false, length = 5)
    private String rowLabel;

    @Column(name = "SeatNumber", nullable = false)
    private Integer seatNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SeatTypeId", nullable = false)
    private SeatType seatType;

    @Column(name = "IsActive", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "seat", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<ShowtimeSeat> showtimeSeats = new LinkedHashSet<>();

    @Transient
    @JsonIgnore
    public String getResolvedCoupleGroupId() {
        if (seatType != null
                && seatType.getName() != null
                && "Couple".equalsIgnoreCase(seatType.getName())
                && rowLabel != null
                && seatNumber != null) {
            int baseSeatNumber = seatNumber % 2 == 0 ? seatNumber - 1 : seatNumber;
            return rowLabel + "-" + baseSeatNumber;
        }
        return null;
    }
}
