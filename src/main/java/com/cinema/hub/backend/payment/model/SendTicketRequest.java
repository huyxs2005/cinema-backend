package com.cinema.hub.backend.payment.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendTicketRequest {

    @NotNull
    private Integer bookingId;

    @NotBlank
    @Email
    private String email;
}
