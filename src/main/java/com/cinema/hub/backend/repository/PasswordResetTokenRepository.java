package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.PasswordResetToken;
import com.cinema.hub.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findTopByUserAndTokenAndUsedAtIsNullAndExpiresAtAfter(
            UserAccount user,
            String token,
            OffsetDateTime now);
}
