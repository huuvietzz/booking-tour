package com.example.tour.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

// Đặt lại mk khi bị quên, click quên mk, nhập email, nhấn gửi. Mở email, nhấn nút đặt lại mk, rồi nhập mk mới, xác nhận mk mới
public class ResetPasswordRequest {

    /** Đọc từ query string của trang reset-password.html, không phải user gõ. */
    @NotBlank(message = "Token không được để trống")
    private String token;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isPasswordMatching() {
        if (newPassword == null || confirmPassword == null) return true;
        return newPassword.equals(confirmPassword);
    }
}