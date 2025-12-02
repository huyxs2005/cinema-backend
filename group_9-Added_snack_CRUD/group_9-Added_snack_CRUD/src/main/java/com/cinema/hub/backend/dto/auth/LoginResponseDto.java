package com.cinema.hub.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean active;
    private String message;
}
