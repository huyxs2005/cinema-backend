package com.cinema.hub.backend.service.view;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SeatView {
    private Integer seatId;
    private String label;
    private String rowLabel;
    private Integer seatNumber;
    private String type;
    private BigDecimal price;
    private String status;
    private String coupleGroupId;
    private boolean selectable;
    private Integer holdUserId;
}
