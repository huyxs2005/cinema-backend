package com.cinema.hub.backend.mapper;

import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.entity.Auditorium;
import org.springframework.stereotype.Component;

@Component
public class AuditoriumMapper {

    public AuditoriumResponse toResponse(Auditorium auditorium) {
        if (auditorium == null) {
            return null;
        }
        return AuditoriumResponse.builder()
                .id(auditorium.getId())
                .name(auditorium.getName())
                .numberOfRows(auditorium.getNumberOfRows())
                .numberOfColumns(auditorium.getNumberOfColumns())
                .active(auditorium.getActive())
                .build();
    }
}
