package com.cinema.hub.backend.mapper;

import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.util.SeatLayoutCalculator;
import org.springframework.stereotype.Component;

@Component
public class AuditoriumMapper {

    public AuditoriumResponse toResponse(Auditorium auditorium,
                                         SeatLayoutCalculator.SeatRowDistribution distribution) {
        if (auditorium == null) {
            return null;
        }
        int standardRows = distribution != null ? distribution.standardRows() : 0;
        int vipRows = distribution != null ? distribution.vipRows() : 0;
        int coupleRows = distribution != null ? distribution.coupleRows() : 0;
        return AuditoriumResponse.builder()
                .id(auditorium.getId())
                .name(auditorium.getName())
                .numberOfRows(auditorium.getNumberOfRows())
                .numberOfColumns(auditorium.getNumberOfColumns())
                .normalRowCount(standardRows)
                .vipRowCount(vipRows)
                .coupleRowCount(coupleRows)
                .active(auditorium.getActive())
                .build();
    }
}
