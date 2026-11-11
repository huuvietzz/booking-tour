package com.example.tour.user.dto.response;

import com.example.tour.common.constant.Gender;
import com.example.tour.user.constant.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class StaffResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatarUrl;
    private Role role;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** true = dòng này là chính admin đang đăng nhập → FE khoá công tắc, nút xoá, ô vai trò */
    private boolean self;
}
