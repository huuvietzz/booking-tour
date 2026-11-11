package com.example.tour.user.dto.request;

import com.example.tour.common.constant.Gender;
import com.example.tour.user.constant.Role;
import lombok.Data;

/** Bộ lọc trang quản lý nhân viên: ?keyword=&role=STAFF&isActive=&gender= */
@Data
public class StaffFilterRequest {
    private String keyword;      // họ tên, email hoặc số điện thoại
    private Role role;           // ADMIN / STAFF (CUSTOMER bị bỏ qua)
    private Boolean isActive;
    private Gender gender;
}
