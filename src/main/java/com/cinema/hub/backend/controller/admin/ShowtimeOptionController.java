package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.showtime.AuditoriumOptionDto;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.repository.AuditoriumRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/showtime-options")
public class ShowtimeOptionController {

    private final AuditoriumRepository auditoriumRepository;
    public ShowtimeOptionController(AuditoriumRepository auditoriumRepository) {
        this.auditoriumRepository = auditoriumRepository;
    }

    @GetMapping("/auditoriums")
    public ResponseEntity<List<AuditoriumOptionDto>> getAuditoriums() {
        List<AuditoriumOptionDto> data = auditoriumRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapAuditorium)
                .toList();
        return ResponseEntity.ok(data);
    }

    private AuditoriumOptionDto mapAuditorium(Auditorium auditorium) {
        Integer rows = auditorium.getNumberOfRows();
        Integer cols = auditorium.getNumberOfColumns();
        Integer totalSeats = calculateDisplaySeats(rows, cols);
        return AuditoriumOptionDto.builder()
                .id(auditorium.getId())
                .name(auditorium.getName())
                .totalSeats(totalSeats)
                .build();
    }

    private Integer calculateDisplaySeats(Integer rows, Integer columns) {
        if (rows == null || columns == null) {
            return null;
        }
        int totalSeats = rows * columns;
        if (columns % 2 != 0) {
            totalSeats -= determineCoupleRows(rows);
        }
        return Math.max(0, totalSeats);
    }

    private int determineCoupleRows(int totalRows) {
        if (totalRows >= 20) {
            return 2;
        }
        if (totalRows >= 10) {
            return 1;
        }
        return 0;
    }
}
