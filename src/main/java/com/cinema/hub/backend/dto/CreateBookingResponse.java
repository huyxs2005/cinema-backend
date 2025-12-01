package com.cinema.hub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CreateBookingResponse {
    private Integer bookingId;
    private String bookingCode;
}
