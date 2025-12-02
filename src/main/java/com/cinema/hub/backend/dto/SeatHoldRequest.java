package com.cinema.hub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatHoldRequest {

    @NotNull
    @Min(1)
    private Integer showtimeId;

    private Integer userId;

    @NotEmpty
    private List<@NotNull @Min(1) Integer> seatIds;

    private String previousHoldToken;
}
