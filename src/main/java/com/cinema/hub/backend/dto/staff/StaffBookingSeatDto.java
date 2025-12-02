package com.cinema.hub.backend.dto.staff;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
public class StaffBookingSeatDto {
    private Integer seatId;
    private String seatLabel;
    private String seatType;
    private BigDecimal finalPrice;
    private boolean checkedIn;
    private OffsetDateTime checkedInAt;
}
