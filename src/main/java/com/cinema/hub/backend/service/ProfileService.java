package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.profile.ChangePasswordRequestDto;
import com.cinema.hub.backend.dto.profile.UpdateProfileRequestDto;
import com.cinema.hub.backend.dto.profile.UserProfileDto;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.repository.UserAccountRepository;
import com.cinema.hub.backend.web.view.BookingHistoryView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10,11}$");

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingService bookingService;

    public UserProfileDto getProfile(Integer userId) {
        UserAccount user = findUser(userId);
        return mapToDto(user);
    }

    public UserProfileDto updateProfile(Integer userId, UpdateProfileRequestDto request) {
        UserAccount user = findUser(userId);
        user.setFullName(buildFullName(request.getFirstName(), request.getLastName()));
        String normalizedPhone = normalizePhone(request.getPhone());
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (10-11 chữ số).");
        }
        userAccountRepository.findByPhone(normalizedPhone)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Số điện thoại đã được sử dụng!");
                });
        user.setPhone(normalizedPhone);
        UserAccount saved = userAccountRepository.save(user);
        return mapToDto(saved);
    }

    public void changePassword(Integer userId, ChangePasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        UserAccount user = findUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);
    }

    public List<BookingHistoryView> getBookingHistory(Integer userId) {
        UserAccount user = findUser(userId);
        return bookingService.getBookingHistory(user);
    }

    private UserAccount findUser(Integer userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
    }

    private UserProfileDto mapToDto(UserAccount user) {
        String[] parts = splitFullName(user.getFullName());
        return UserProfileDto.builder()
                .userId(user.getId())
                .firstName(parts[0])
                .lastName(parts[1])
                .phone(user.getPhone())
                .email(user.getEmail())
                .build();
    }

    private String buildFullName(String firstName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(firstName)) {
            builder.append(firstName.trim());
        }
        if (StringUtils.hasText(lastName)) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        return builder.toString().trim();
    }

    private String[] splitFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return new String[]{"", ""};
        }
        String[] tokens = fullName.trim().split("\\s+");
        if (tokens.length == 1) {
            return new String[]{"", tokens[0]};
        }
        String lastName = tokens[tokens.length - 1];
        String firstName = String.join(" ", Arrays.copyOf(tokens, tokens.length - 1));
        return new String[]{firstName, lastName};
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
}
