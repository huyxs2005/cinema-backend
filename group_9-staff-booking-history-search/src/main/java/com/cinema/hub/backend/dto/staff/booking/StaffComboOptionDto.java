package com.cinema.hub.backend.dto.staff.booking;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffComboOptionDto {

    private final Integer comboId;
    private final String name;
    private final BigDecimal price;
    private final String description;
}
