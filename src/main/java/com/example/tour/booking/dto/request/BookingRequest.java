package com.example.tour.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull(message = "Vui lòng chọn lịch khởi hành")
    private Integer scheduleId;

    private Integer promotionId;

    @NotBlank(message = "Vui lòng nhập họ tên người liên hệ")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    private String contactName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String contactEmail;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0")
    private String contactPhone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String contactAddress;

    @NotNull
    @Min(value = 1, message = "Phải có ít nhất 1 người lớn")
    private Integer totalAdults;

    @NotNull
    @Min(0)
    private Integer totalChildren = 0;

    @NotNull
    @Min(0)
    private Integer totalToddlers = 0;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String note;

    @Valid
    @NotEmpty(message = "Vui lòng nhập thông tin hành khách")
    @Size(max = 50)
    private List<ParticipantRequest> participants = new ArrayList<>();
}
