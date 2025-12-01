package com.cinema.hub.backend.web.view;

import com.cinema.hub.backend.entity.UserAccount;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class UserProfileView {
    private final UserAccount user;
    private final String preferredTheater;
    private final String membershipTier;

    public UserProfileView(UserAccount user, String preferredTheater, String membershipTier) {
        this.user = user;
        this.preferredTheater = preferredTheater;
        this.membershipTier = membershipTier;
    }

    public String getFullName() {
        return user.getFullName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getPhone() {
        return user.getPhone();
    }

    public OffsetDateTime getJoinedAt() {
        return user.getCreatedAt();
    }
}
