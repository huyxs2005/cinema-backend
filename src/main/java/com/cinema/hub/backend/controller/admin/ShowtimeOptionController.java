package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.showtime.AuditoriumOptionDto;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.repository.AuditoriumRepository;
import com.cinema.hub.backend.repository.SeatRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/showtime-options")
public class ShowtimeOptionController {

    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;

    public ShowtimeOptionController(AuditoriumRepository auditoriumRepository,
                                    SeatRepository seatRepository) {
        this.auditoriumRepository = auditoriumRepository;
        this.seatRepository = seatRepository;
    }

    @GetMapping("/auditoriums")
    public ResponseEntity<List<AuditoriumOptionDto>> getAuditoriums() {
        List<AuditoriumOptionDto> data = auditoriumRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapAuditorium)
                .toList();
        return ResponseEntity.ok(data);
    }

    private AuditoriumOptionDto mapAuditorium(Auditorium auditorium) {
        long totalSeats = seatRepository.countByAuditorium_Id(auditorium.getId());
        return AuditoriumOptionDto.builder()
                .id(auditorium.getId())
                .name(auditorium.getName())
                .totalSeats(Math.toIntExact(totalSeats))
                .build();
    }
}
