package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.SeatMapItemDto;
import com.cinema.hub.backend.service.view.SeatLayoutView;
import com.cinema.hub.backend.service.view.SeatRowView;
import com.cinema.hub.backend.service.view.SeatView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeatService {

    private static final int DEFAULT_SELECTION_LIMIT = 6;

    private final SeatReservationService seatReservationService;

    public SeatLayoutView buildSeatLayout(int showtimeId) {
        List<SeatMapItemDto> seatDtos = seatReservationService.getSeatMap(showtimeId);
        Map<String, List<SeatView>> grouped = new LinkedHashMap<>();
        for (SeatMapItemDto dto : seatDtos) {
            String rowLabel = dto.getRowLabel();
            grouped.computeIfAbsent(rowLabel, key -> new ArrayList<>())
                    .add(toSeatView(dto));
        }

        List<SeatRowView> rows = grouped.entrySet().stream()
                .map(entry -> new SeatRowView(entry.getKey(), entry.getValue()))
                .toList();
        return new SeatLayoutView(rows, DEFAULT_SELECTION_LIMIT);
    }

    private SeatView toSeatView(SeatMapItemDto dto) {
        String normalizedStatus = dto.getStatus() != null
                ? dto.getStatus().toUpperCase(Locale.ROOT)
                : "DISABLED";
        boolean isSelectable = dto.isSelectable();
        String type = dto.getSeatType() != null ? dto.getSeatType() : "Standard";
        return SeatView.builder()
                .seatId(dto.getSeatId())
                .label(dto.getSeatLabel())
                .rowLabel(dto.getRowLabel())
                .seatNumber(dto.getSeatNumber())
                .type(type)
                .price(dto.getPrice())
                .status(normalizedStatus)
                .coupleGroupId(dto.getCoupleGroupId())
                .selectable(isSelectable)
                .holdUserId(dto.getHoldUserId())
                .build();
    }
}
