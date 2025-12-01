package com.cinema.hub.backend.web.view;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SeatSelectionItemView {
    private final String label;
    private final BigDecimal price;
}
