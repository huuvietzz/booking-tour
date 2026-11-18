package com.example.tour.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import org.springframework.core.io.ByteArrayResource;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

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

    public void sendHtmlEmail(String to, String subject, String html) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(from, "VietTrip");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    /**
     * Gửi email HTML kèm ảnh nhúng. Trong HTML dùng <img src="cid:TÊN">, Map: TÊN → byte[] PNG.
     * Ảnh nhúng (inline) hiện ngay trong nội dung, không phải file đính kèm để tải.
     */
    public void sendHtmlEmail(String to, String subject, String html, Map<String, byte[]> inlineImages)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from, "VietTrip");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        for (Map.Entry<String, byte[]> img : inlineImages.entrySet()) {
            helper.addInline(img.getKey(), new ByteArrayResource(img.getValue()), "image/png");
        }
        mailSender.send(message);
    }
}
