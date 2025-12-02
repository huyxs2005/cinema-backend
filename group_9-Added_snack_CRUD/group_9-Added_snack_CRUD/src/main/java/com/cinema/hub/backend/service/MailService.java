package com.cinema.hub.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@cinemahub.local}")
    private String fromAddress;

    public void sendPasswordReset(String to, String token) {
        String subject = "Cinema HUB - Password Reset";
        String text = """
                Xin chào,

                Bạn hoặc ai đó đã yêu cầu đổi mật khẩu cho tài khoản Cinema HUB.
                Đây là mã xác thực của bạn: %s

                Mã này sẽ hết hạn sau 15 phút. Nếu bạn không yêu cầu thao tác này hãy bỏ qua email.

                Trân trọng,
                Cinema HUB
                """.formatted(token);
        sendMail(to, subject, text);
    }

    public void sendRegistrationConfirmation(String to, String fullName) {
        String subject = "Chào mừng tới Cinema HUB";
        String text = """
                Xin chào %s,

                Tài khoản Cinema HUB của bạn đã được tạo thành công.
                Hãy đăng nhập để khám phá các suất chiếu mới nhất!

                Cinema HUB
                """.formatted(fullName);
        sendMail(to, subject, text);
    }

    private void sendMail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Không thể gửi email tới {}: {}", to, ex.getMessage());
        }
    }
}
