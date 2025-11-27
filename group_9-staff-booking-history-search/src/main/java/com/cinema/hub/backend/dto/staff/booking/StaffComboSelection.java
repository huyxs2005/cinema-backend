package com.cinema.hub.backend.dto.staff.booking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffComboSelection {

    private final Integer comboId;
    private final Integer quantity;
}
