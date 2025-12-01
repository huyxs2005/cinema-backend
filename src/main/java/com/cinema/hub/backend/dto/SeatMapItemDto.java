package com.cinema.hub.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class SeatMapItemDto {
    private Integer seatId;
    private String rowLabel;
    private Integer seatNumber;
    private String seatLabel;
    private String seatType;
    private String coupleGroupId;
    private BigDecimal price;
    private String status;
    private boolean selectable;
    private Integer holdUserId;
}
