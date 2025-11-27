package com.cinema.hub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String token;

    @Size(min = 6)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
