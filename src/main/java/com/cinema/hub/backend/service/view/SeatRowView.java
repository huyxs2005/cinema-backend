package com.cinema.hub.backend.service.view;

import java.util.List;

public record SeatRowView(
        String label,
        List<SeatView> seats
) {
}
