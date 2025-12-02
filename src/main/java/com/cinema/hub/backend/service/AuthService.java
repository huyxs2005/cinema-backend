package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.auth.ForgotPasswordRequestDto;
import com.cinema.hub.backend.dto.auth.RegisterRequestDto;
import com.cinema.hub.backend.dto.auth.ResetPasswordRequestDto;
import com.cinema.hub.backend.dto.auth.VerifyResetTokenRequestDto;
import com.cinema.hub.backend.entity.PasswordResetToken;
import com.cinema.hub.backend.entity.Role;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.repository.PasswordResetTokenRepository;
import com.cinema.hub.backend.repository.RoleRepository;
import com.cinema.hub.backend.repository.UserAccountRepository;
import com.cinema.hub.backend.util.TimeProvider;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String DEFAULT_ROLE = "User";
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 15;  //otp email expiery countdown in minute
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Random random = new Random();

    public UserAccount register(RegisterRequestDto request) {
        validateRegistration(request);
        String normalizedPhone = normalizeAndValidatePhone(request.getPhone());

        if (userAccountRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        try {
            userAccountRepository.findByPhone(normalizedPhone)
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
                    });
        } catch (IncorrectResultSizeDataAccessException ex) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
        }

        String roleName = StringUtils.hasText(request.getRole()) ? request.getRole() : DEFAULT_ROLE;
        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + roleName));

        UserAccount user = UserAccount.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(normalizedPhone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .createdAt(TimeProvider.now())
                .build();

        UserAccount saved = userAccountRepository.save(user);
        mailService.sendRegistrationConfirmation(saved.getEmail(), saved.getFullName());
        return saved;
    }

    public PasswordResetToken createPasswordResetToken(ForgotPasswordRequestDto request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        String token = String.format("%06d", random.nextInt(1_000_000));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(TimeProvider.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES))
                .build();

        PasswordResetToken saved = passwordResetTokenRepository.save(resetToken);
        mailService.sendPasswordReset(user.getEmail(), token);
        log.info("Password reset token for {} is {}", user.getEmail(), token);
        return saved;
    }

    public void resetPassword(ResetPasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByUserAndTokenAndUsedAtIsNullAndExpiresAtAfter(
                        user,
                        request.getToken(),
                        TimeProvider.now())
                .orElseThrow(() -> new IllegalArgumentException("Mã nhập không hợp lệ"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);

        token.setUsedAt(TimeProvider.now());
        passwordResetTokenRepository.save(token);
    }

    private void validateRegistration(RegisterRequestDto request) {
        if (!StringUtils.hasText(request.getPassword()) ||
                !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        normalizeAndValidatePhone(request.getPhone());
    }

    private String normalizeAndValidatePhone(String phone) {
        String normalized = normalizePhone(phone);
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (10-11 chữ số).");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }

    public void verifyResetToken(VerifyResetTokenRequestDto request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        passwordResetTokenRepository
                .findTopByUserAndTokenAndUsedAtIsNullAndExpiresAtAfter(
                        user,
                        request.getToken(),
                        TimeProvider.now())
                .orElseThrow(() -> new IllegalArgumentException("Mã nhập không hợp lệ"));
    }

    public UserAccount updateLastLogin(Integer userId) {
        return userAccountRepository.findById(userId)
                .map(user -> {
                    user.setLastLoginAt(TimeProvider.now());
                    return userAccountRepository.save(user);
                })
                .orElse(null);
    }
}
