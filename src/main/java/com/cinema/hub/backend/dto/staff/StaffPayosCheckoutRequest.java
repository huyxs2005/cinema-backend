package com.cinema.hub.backend.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffPayosCheckoutRequest {

    @NotBlank
    @Email
    private String email;
}

