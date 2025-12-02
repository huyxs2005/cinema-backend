package com.cinema.hub.backend.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalkInBookingRequest {

    @NotNull
    private Integer showtimeId;

    @NotEmpty
    private List<Integer> seatIds;

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    @Email
    private String email;

    private BigDecimal discountPercent;

    private String discountCode;

    @NotNull
    private BigDecimal finalPrice;

    @NotBlank
    private String paymentMethod;
}
