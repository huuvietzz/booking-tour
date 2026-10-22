package com.example.tour.user.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Chỉ trả về sau khi ĐĂNG NHẬP thành công, không trả khi đăng ký. */
@Getter
@Builder
public class AuthResponse {
    private Integer userId;
    private String token;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
}
