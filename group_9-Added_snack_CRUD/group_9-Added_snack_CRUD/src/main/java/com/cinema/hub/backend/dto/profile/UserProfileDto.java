package com.cinema.hub.backend.dto.profile;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserProfileDto {

    private Integer userId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
}
