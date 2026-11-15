package com.example.tour.booking.dto.request;

import com.example.tour.booking.constant.ParticipantType;
import com.example.tour.common.constant.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ParticipantRequest {

    @NotBlank(message = "Vui lòng nhập họ tên hành khách")
    @Size(max = 255, message = "Họ tên không vượt quá 255 ký tự")
    private String fullName;

    @NotNull(message = "Vui lòng nhập ngày sinh hành khách")
    @Past(message = "Ngày sinh phải trước hôm nay")
    private LocalDate dateOfBirth;

    private Gender gender;

    @NotNull(message = "Thiếu loại hành khách")
    private ParticipantType participantType;

    @Pattern(regexp = "^$|^0[0-9]{9}$", message = "Số điện thoại hành khách không hợp lệ")
    private String phoneNumber;

    /** Chỉ người lớn được chọn phòng đơn */
    private Boolean singleRoom;
}
