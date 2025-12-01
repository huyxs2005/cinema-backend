package com.cinema.hub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
public class SeatHoldResponse {
    private String holdToken;
    private OffsetDateTime expiresAt;
}
