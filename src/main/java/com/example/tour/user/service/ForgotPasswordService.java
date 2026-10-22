package com.example.tour.user.service;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.user.constant.TokenType;
import com.example.tour.user.dto.request.ForgotPasswordRequest;
import com.example.tour.user.dto.response.MessageResponse;
import com.example.tour.user.dto.request.ResetPasswordRequest;
import com.example.tour.user.entity.User;
import com.example.tour.user.entity.Token;
import com.example.tour.user.repository.UserRepository;
import com.example.tour.user.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final UserTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    // nhấn quên mật khẩu -> nhập email -> nhấn gửi
    @Transactional
    public MessageResponse processForgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_OR_INACTIVE));

        // Chưa xác thực email thì không gửi link đặt lại mật khẩu
        if (!user.isEmailVerified() || !user.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND_OR_INACTIVE);
        }

        tokenRepository.deleteByUser_UserIdAndTokenType(
                user.getUserId(), TokenType.PASSWORD_RESET);

        Token resetToken = new Token();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setTokenType(TokenType.PASSWORD_RESET);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/user/reset-password.html?token=" + resetToken.getToken();
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return new MessageResponse(
                "Gửi yêu cầu thành công. Vui lòng kiểm tra email để tiếp tục");
    }

    // nhập mk mới, xác thực mk moi, nhấn đặt lại mk
    @Transactional
    public MessageResponse processResetPassword(ResetPasswordRequest request) {
        // Tra kèm tokenType: token xác thực email KHÔNG dùng để đổi mật khẩu được
        Token resetToken = tokenRepository
                .findByTokenAndTokenType(request.getToken(), TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (resetToken.isUsed()) {
            throw new AppException(ErrorCode.TOKEN_ALREADY_USED);
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return new MessageResponse("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
    }
}
