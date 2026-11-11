package com.example.tour.user.dto.response;

import com.example.tour.common.constant.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Trả cho trang quản lý khách hàng. Không bao giờ có password */
@Getter
@Builder
public class CustomerResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatarUrl;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long bookingCount;       // số đơn đặt tour (mọi trạng thái)
}
