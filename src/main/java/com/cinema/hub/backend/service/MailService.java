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
        String subject = "Mã xác thực đặt lại mật khẩu Cinema HUB";
        String text = """
                Xin chào %s,

                Chúng tôi vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản Cinema HUB của bạn.

                Để hoàn tất quá trình này, vui lòng sử dụng mã xác thực dưới đây:

                %s

                Lưu ý: Mã này sẽ hết hạn sau 15 phút.

                Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với bộ phận hỗ trợ nếu bạn nghi ngờ có truy cập trái phép.

                Trân trọng,
                Đội ngũ Cinema HUB
                """.formatted(to, token);
        sendMail(to, subject, text);
    }

    public void sendRegistrationConfirmation(String to, String fullName) {
        String subject = "Đăng ký thành công tài khoản Cinema HUB";
        String text = """
                Kính gửi %s,

                Cảm ơn bạn đã lựa chọn dịch vụ của Cinema HUB. Chúng tôi xin thông báo tài khoản của bạn đã được khởi tạo thành công và sẵn sàng sử dụng.

                Với tài khoản này, bạn có thể quản lý lịch sử đặt vé, và cập nhật lịch chiếu phim mới nhất một cách thuận tiện.

                Nếu cần hỗ trợ trong quá trình sử dụng, vui lòng liên hệ với chúng tôi qua email này.

                Chúc bạn có những trải nghiệm xem phim thú vị.

                Trân trọng,
                Ban Quản trị Cinema HUB
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
