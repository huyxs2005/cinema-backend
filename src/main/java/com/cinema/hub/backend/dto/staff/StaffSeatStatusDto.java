package com.cinema.hub.backend.dto.staff;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSeatStatusDto {
    private Integer seatId;
    private String seatLabel;
    private String seatType;
    private BigDecimal price;
    private String status;
    private boolean selectable;
    private Integer holdUserId;
}
