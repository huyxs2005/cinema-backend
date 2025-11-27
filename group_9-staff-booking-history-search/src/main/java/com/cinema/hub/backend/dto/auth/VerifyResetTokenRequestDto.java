package com.cinema.hub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyResetTokenRequestDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String token;
}
