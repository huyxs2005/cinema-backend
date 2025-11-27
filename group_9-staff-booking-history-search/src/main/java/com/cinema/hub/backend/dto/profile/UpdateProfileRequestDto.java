package com.cinema.hub.backend.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequestDto {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^\\d{10,11}$", message = "Số điện thoại phải gồm 10-11 chữ số")
    private String phone;
}
