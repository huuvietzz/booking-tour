package com.example.tour.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Khác ResetPasswordRequest: ở đây user đang đăng nhập, chứng minh danh tính
 * bằng mật khẩu hiện tại chứ không phải bằng token trong email.
 */
@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isPasswordMatching() {
        if (newPassword == null || confirmPassword == null) return true;
        return newPassword.equals(confirmPassword);
    }

    @AssertTrue(message = "Mật khẩu mới phải khác mật khẩu hiện tại")
    public boolean isPasswordChanged() {
        if (newPassword == null || currentPassword == null) return true;
        return !newPassword.equals(currentPassword);
    }
}
