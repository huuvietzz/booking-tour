package com.example.tour.user.service;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.security.service.JwtService;
import com.example.tour.user.constant.Role;
import com.example.tour.user.constant.TokenType;
import com.example.tour.user.dto.request.AdminLoginRequest;
import com.example.tour.user.dto.request.LoginRequest;
import com.example.tour.user.dto.request.RegisterRequest;
import com.example.tour.user.dto.response.AuthResponse;
import com.example.tour.user.dto.response.MessageResponse;
import com.example.tour.user.entity.User;
import com.example.tour.user.entity.Token;
import com.example.tour.user.repository.UserRepository;
import com.example.tour.user.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;


    @Value("${app.base-url}")
    private String baseUrl;

    private static final int VERIFY_TOKEN_HOURS = 24;

    // =====================================================================
    // ĐĂNG KÝ
    // =====================================================================

    /**
     * KHÔNG trả JWT. Tài khoản có email_verified = false, chưa đăng nhập được
     * cho tới khi bấm link trong mail. Trả token ở đây sẽ làm xác thực email
     * thành vô nghĩa, vì có token là dùng được API.
     */
    @Transactional
    public MessageResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());          // có thể null
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Server tự quyết, không nhận từ client:
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setEmailVerified(false);

        userRepository.save(user);
        sendVerificationToken(user);

        return new MessageResponse(
                "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.");
    }

    /** Sinh token xác thực và gửi mail. Dùng cho đăng ký. */
    private void sendVerificationToken(User user) {
        tokenRepository.deleteByUser_UserIdAndTokenType(
                user.getUserId(), TokenType.EMAIL_VERIFICATION);

        Token token = new Token();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setTokenType(TokenType.EMAIL_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().plusHours(VERIFY_TOKEN_HOURS));
        token.setUsed(false);
        tokenRepository.save(token);

        // Trỏ vào TRANG HTML, không trỏ vào API. Trang đó sẽ fetch() tới API.
        String verifyLink = baseUrl + "/user/verify-email.html?token=" + token.getToken();
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verifyLink);
    }

    // =====================================================================
    // XÁC THỰC EMAIL: Dùng lúc người dùng mở mail, click link để xác thực email từ đăng ky gui ve
    // =====================================================================

    @Transactional
    public MessageResponse verifyEmail(String token) {
        Token userToken = tokenRepository
                .findByTokenAndTokenType(token, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (userToken.isUsed()) {
            throw new AppException(ErrorCode.TOKEN_ALREADY_USED);
        }
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = userToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        userToken.setUsed(true);
        tokenRepository.save(userToken);

        return new MessageResponse("Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.");
    }


    /** Trang user: email hoặc số điện thoại. */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = authenticate(request.getEmailOrPhone(), request.getPassword());

        if (user.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        return buildAuthResponse(user);
    }

    /** Trang quản trị: ADMIN và STAFF dùng chung, chỉ đăng nhập bằng email, không có đăng ký */
    @Transactional
    public AuthResponse adminLogin(AdminLoginRequest request) {
        User user = authenticate(request.getEmail(), request.getPassword());

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            // Cùng lỗi với sai mật khẩu, để không lộ tài khoản nào là admin
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        return buildAuthResponse(user);
    }

    // =====================================================================

    private User authenticate(String principal, String rawPassword) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(principal, rawPassword));
        } catch (LockedException e) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            // User.isEnabled() = isActive && emailVerified
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        } catch (BadCredentialsException | InternalAuthenticationServiceException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmailOrPhoneNumber(principal)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getUserId())
                .token(jwtService.generateToken(user))
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
