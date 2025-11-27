package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.auth.ForgotPasswordRequestDto;
import com.cinema.hub.backend.dto.auth.LoginRequestDto;
import com.cinema.hub.backend.dto.auth.LoginResponseDto;
import com.cinema.hub.backend.dto.auth.RegisterRequestDto;
import com.cinema.hub.backend.dto.auth.ResetPasswordRequestDto;
import com.cinema.hub.backend.dto.auth.VerifyResetTokenRequestDto;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.security.UserPrincipal;
import com.cinema.hub.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UserAccount user = principal.getUser();

            LoginResponseDto response = LoginResponseDto.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .role(user.getRole() != null ? user.getRole().getName() : "User")
                    .active(user.isActive())
                    .message("Login successful")
                    .build();
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Thông tin đăng nhập không đúng"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        UserAccount user = authService.register(request);
        LoginResponseDto response = LoginResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getName() : "User")
                .active(user.isActive())
                .message("Registration successful")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        var token = authService.createPasswordResetToken(request);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Reset code sent to your email",
                        "resetToken", token.getToken(),
                        "expiresAt", token.getExpiresAt().toString()
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(@Valid @RequestBody VerifyResetTokenRequestDto request) {
        authService.verifyResetToken(request);
        return ResponseEntity.ok(Map.of("message", "Token verified"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Dữ liệu không hợp lệ";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message));
    }
}
