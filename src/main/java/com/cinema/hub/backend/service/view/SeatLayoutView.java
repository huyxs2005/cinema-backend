package com.cinema.hub.backend.service.view;

import java.util.List;

public record SeatLayoutView(
        List<SeatRowView> rows,
        int maxSelectable
) {
}
