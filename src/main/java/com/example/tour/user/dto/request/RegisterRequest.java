package com.example.tour.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên quá dài")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email quá dài")
    private String email;

    @Size(max = 255, message = "Địa chỉ quá dài")
    private String address;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    /** Checkbox điều khoản. Frontend disable nút, backend vẫn phải kiểm lại. */
    @AssertTrue(message = "Bạn phải đồng ý với điều khoản sử dụng")
    private boolean termsAccepted;

    /** Kiểm tra hai ô mật khẩu khớp nhau, chạy cùng lúc với các @NotBlank khác. */
    @AssertTrue(message = "Mật khẩu nhập lại không khớp")
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) return true; // để @NotBlank báo trước
        return password.equals(confirmPassword);
    }
}
