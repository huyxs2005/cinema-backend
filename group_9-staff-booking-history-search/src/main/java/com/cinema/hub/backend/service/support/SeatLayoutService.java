package com.cinema.hub.backend.service.support;

import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.entity.Seat;
import com.cinema.hub.backend.entity.SeatType;
import com.cinema.hub.backend.entity.Showtime;
import com.cinema.hub.backend.entity.ShowtimeSeat;
import com.cinema.hub.backend.repository.SeatRepository;
import com.cinema.hub.backend.repository.SeatTypeRepository;
import com.cinema.hub.backend.repository.ShowtimeSeatRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SeatLayoutService {

    private static final String DEFAULT_SEAT_STATUS = "Available";
    private static final String DEFAULT_SEAT_TYPE_NAME = "Standard";

    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;

    @Transactional
    public List<Seat> ensureSeatTemplates(Auditorium auditorium) {
        if (auditorium == null || auditorium.getId() == null) {
            return List.of();
        }
        List<Seat> existing = seatRepository
                .findByAuditorium_IdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(auditorium.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        int rows = positiveValue(auditorium.getNumberOfRows());
        int cols = positiveValue(auditorium.getNumberOfColumns());
        if (rows <= 0 || cols <= 0) {
            return List.of();
        }
        SeatType defaultSeatType = resolveDefaultSeatType();
        List<Seat> generated = new ArrayList<>(rows * cols);
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            String rowLabel = buildRowLabel(rowIndex);
            for (int colIndex = 1; colIndex <= cols; colIndex++) {
                generated.add(Seat.builder()
                        .auditorium(auditorium)
                        .rowLabel(rowLabel)
                        .seatNumber(colIndex)
                        .seatType(defaultSeatType)
                        .active(Boolean.TRUE)
                        .build());
            }
        }
        saveSeatsIgnoringConflicts(generated);
        return seatRepository.findByAuditorium_IdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(auditorium.getId());
    }

    @Transactional
    public List<ShowtimeSeat> ensureShowtimeSeats(Showtime showtime) {
        if (showtime == null || showtime.getId() == null) {
            return List.of();
        }
        List<ShowtimeSeat> existing = showtimeSeatRepository
                .findByShowtime_IdOrderBySeat_RowLabelAscSeat_SeatNumberAsc(showtime.getId());
        if (!existing.isEmpty()) {
            return existing;
        }
        List<Seat> seatTemplates = ensureSeatTemplates(showtime.getAuditorium());
        if (seatTemplates.isEmpty()) {
            return List.of();
        }
        List<ShowtimeSeat> snapshots = seatTemplates.stream()
                .map(seat -> ShowtimeSeat.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .effectivePrice(calculateSeatPrice(showtime.getBasePrice(), seat))
                        .status(DEFAULT_SEAT_STATUS)
                        .build())
                .toList();
        saveShowtimeSeatsIgnoringConflicts(snapshots);
        return showtimeSeatRepository
                .findByShowtime_IdOrderBySeat_RowLabelAscSeat_SeatNumberAsc(showtime.getId());
    }

    private void saveSeatsIgnoringConflicts(List<Seat> seats) {
        if (seats.isEmpty()) {
            return;
        }
        try {
            seatRepository.saveAll(seats);
        } catch (DataIntegrityViolationException ignored) {
            // Another transaction has already generated the seats. Reload below.
        }
    }

    private void saveShowtimeSeatsIgnoringConflicts(List<ShowtimeSeat> showtimeSeats) {
        if (showtimeSeats.isEmpty()) {
            return;
        }
        try {
            showtimeSeatRepository.saveAll(showtimeSeats);
        } catch (DataIntegrityViolationException ignored) {
            // Ignore concurrent inserts and reload seats for the caller.
        }
    }

    private int positiveValue(Integer value) {
        return value != null && value > 0 ? value : 0;
    }

    private SeatType resolveDefaultSeatType() {
        return seatTypeRepository.findByNameIgnoreCase(DEFAULT_SEAT_TYPE_NAME)
                .or(() -> seatTypeRepository.findFirstByActiveTrueOrderByIdAsc())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Please create at least one active seat type before generating seats automatically."));
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, Seat seat) {
        BigDecimal multiplier = seat.getSeatType() != null && seat.getSeatType().getPriceMultiplier() != null
                ? seat.getSeatType().getPriceMultiplier()
                : BigDecimal.ONE;
        BigDecimal effectiveBase = basePrice != null ? basePrice : BigDecimal.ZERO;
        return effectiveBase.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildRowLabel(int index) {
        StringBuilder label = new StringBuilder();
        int current = index;
        while (current >= 0) {
            int remainder = current % 26;
            label.insert(0, (char) ('A' + remainder));
            current = (current / 26) - 1;
        }
        return label.toString();
    }
}
