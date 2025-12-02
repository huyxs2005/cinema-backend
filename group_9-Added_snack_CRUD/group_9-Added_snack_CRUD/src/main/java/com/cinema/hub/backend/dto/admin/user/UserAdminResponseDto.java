package com.cinema.hub.backend.dto.admin.user;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAdminResponseDto {

    private final Integer userId;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String role;
    private final Boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime lastLoginAt;
}
