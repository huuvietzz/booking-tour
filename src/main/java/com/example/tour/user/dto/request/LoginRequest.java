package com.example.tour.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Trang user: đăng nhập bằng email HOẶC số điện thoại. */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại")
    private String emailOrPhone;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
