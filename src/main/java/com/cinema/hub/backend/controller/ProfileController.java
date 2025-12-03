package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.profile.ChangePasswordRequestDto;
import com.cinema.hub.backend.dto.profile.UpdateProfileRequestDto;
import com.cinema.hub.backend.dto.profile.UserProfileDto;
import com.cinema.hub.backend.service.ProfileService;
import com.cinema.hub.backend.web.view.BookingHistoryView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable Integer userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<PageResponse<BookingHistoryView>> getHistory(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(profileService.getBookingHistory(userId, page, size));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Integer userId,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        profileService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }
}
