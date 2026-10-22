package com.example.tour.user.dto.response;

import com.example.tour.common.constant.Gender;
import com.example.tour.user.constant.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Builder
public class UserResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String avatarUrl;
    private Role role;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}