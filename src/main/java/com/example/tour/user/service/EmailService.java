package com.example.tour.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * @Async để request đăng ký không phải chờ SMTP.
     * Cần @EnableAsync trên class main.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác thực tài khoản - Booking Tour");
        message.setText(
                "Xin chào " + fullName + ",\n\n" +
                        "Cảm ơn bạn đã đăng ký tài khoản tại Booking Tour.\n" +
                        "Nhấn vào link bên dưới để xác thực email và kích hoạt tài khoản:\n\n" +
                        verifyLink + "\n\n" +
                        "Link có hiệu lực trong 24 giờ.\n" +
                        "Nếu bạn không đăng ký tài khoản này, hãy bỏ qua email.\n\n" +
                        "Booking Tour Team"
        );
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Gửi mail xác thực thất bại cho {}", toEmail, e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu - Booking Tour");
        message.setText(
                "Xin chào,\n\n" +
                        "Bạn đã yêu cầu đặt lại mật khẩu. Nhấn vào link bên dưới:\n\n" +
                        resetLink + "\n\n" +
                        "Link có hiệu lực trong 15 phút.\n" +
                        "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n" +
                        "Booking Tour Team"
        );
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Gửi mail reset password thất bại cho {}", toEmail, e);
        }
    }
}
