package com.example.tour.user.dto.request;

import com.example.tour.common.constant.Gender;
import lombok.Data;

/** Bộ lọc trang quản lý khách hàng: ?keyword=&isActive=&emailVerified=&gender= */
@Data
public class CustomerFilterRequest {
    private String keyword;          // họ tên, email hoặc số điện thoại
    private Boolean isActive;        // true = đang hoạt động, false = đã khoá
    private Boolean emailVerified;   // true = đã xác thực, false = chưa
    private Gender gender;
}
