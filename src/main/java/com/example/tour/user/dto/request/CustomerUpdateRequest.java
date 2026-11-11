package com.example.tour.user.dto.request;

import com.example.tour.common.constant.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Sửa thông tin khách. KHÔNG có mật khẩu: admin không đặt mật khẩu thay khách
 * (khách tự đổi trong trang cá nhân hoặc dùng "Quên mật khẩu").
 */
@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên quá dài")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @Size(max = 255, message = "Địa chỉ quá dài")
    private String address;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    private Gender gender;

    private Boolean isActive = true;
}
