package com.cinema.hub.backend.payment.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCheckoutRequest {

    @NotBlank
    private String holdToken;

    private String fullName;

    private String phone;

    @NotBlank
    @Email
    private String email;
}
