package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.dto.SeatMapItemDto;
import com.cinema.hub.backend.dto.staff.StaffSeatStatusDto;
import com.cinema.hub.backend.service.SeatReservationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffSeatService {

    private final SeatReservationService seatReservationService;

    @Transactional(readOnly = true)
    public List<StaffSeatStatusDto> getSeatStatuses(int showtimeId) {
        List<SeatMapItemDto> seatMap = seatReservationService.getSeatMap(showtimeId);
        return seatMap.stream()
                .map(item -> StaffSeatStatusDto.builder()
                        .seatId(item.getSeatId())
                        .seatLabel(item.getSeatLabel())
                        .seatType(item.getSeatType())
                        .price(item.getPrice())
                        .status(item.getStatus())
                        .selectable(item.isSelectable())
                        .holdUserId(item.getHoldUserId())
                        .build())
                .collect(Collectors.toList());
    }
}
