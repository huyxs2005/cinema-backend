package com.cinema.hub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingRequest {

    @NotBlank
    private String holdToken;

    @NotNull
    @Min(1)
    private Integer userId;

    private String paymentMethod;

    @Min(1)
    private Integer createdByStaffId;
}
