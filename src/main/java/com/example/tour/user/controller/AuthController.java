package com.example.tour.user.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.user.dto.request.*;
import com.example.tour.user.dto.response.AuthResponse;
import com.example.tour.user.dto.response.MessageResponse;
import com.example.tour.user.service.AuthService;
import com.example.tour.user.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Toàn bộ endpoint trả JSON theo chuẩn ApiResponse.
 * Frontend là HTML tĩnh trong resources/static, gọi API bằng fetch().
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;

    // ---------- Trang user ----------

    /**
     * Không trả JWT. Tài khoản có email_verified = false,
     * chưa đăng nhập được cho tới khi bấm link trong mail.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody @Valid RegisterRequest request) {
        MessageResponse result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(result.getMessage(), null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    /**
     * Gọi từ verify-email.html sau khi trang đó đọc token trên query string.
     * Link trong email trỏ tới trang HTML, không trỏ thẳng vào đây.
     */
    // Xác thực email sau khi đăng ký xong, click vào link trong email mới đăng nhập được
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody @Valid VerifyEmailRequest request) {
        MessageResponse result = authService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
    }

    // Nhập email khi click quên mk
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        MessageResponse result = forgotPasswordService.processForgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
    }

    // nhập mk mới, xác nhận mk mới
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        MessageResponse result = forgotPasswordService.processResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), null));
    }

    // ---------- Trang admin (không có đăng ký) ----------

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(
            @RequestBody @Valid AdminLoginRequest request) {
        AuthResponse data = authService.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    /**
     * JWT stateless, server không thu hồi token. Client tự xoá khỏi localStorage.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}