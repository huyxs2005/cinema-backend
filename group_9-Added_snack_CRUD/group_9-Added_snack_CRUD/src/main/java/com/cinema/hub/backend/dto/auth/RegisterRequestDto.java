package com.cinema.hub.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDto {

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại phải gồm 10-11 chữ số")
    private String phone;

    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm số và ký tự đặc biệt")
    @Pattern(
            regexp = "^(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm số và ký tự đặc biệt"
    )
    private String password;

    @NotBlank
    private String confirmPassword;

    private String role; // optional override (User / Staff)
}

