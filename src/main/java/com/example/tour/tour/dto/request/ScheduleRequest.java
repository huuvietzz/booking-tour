package com.example.tour.tour.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Thêm / sửa lịch khởi hành. Mã lịch, số chỗ trống, trạng thái do server tự tính. */
@Getter
@Setter
@NoArgsConstructor
public class ScheduleRequest {

    /** Chỉ dùng khi THÊM. Khi sửa, lịch giữ nguyên tour ban đầu (bỏ qua field này). */
    @NotNull(message = "Vui lòng chọn tour")
    private Integer tourId;

    @NotNull(message = "Vui lòng chọn ngày khởi hành")
    private LocalDate departureDate;

    /** Không gửi → tự tính theo thời gian tour (VD 3N2Đ: đi ngày 13 → về ngày 15) */
    private LocalDate returnDate;

    @NotNull(message = "Vui lòng nhập tổng số chỗ")
    @Min(value = 1, message = "Tổng số chỗ phải từ 1 trở lên")
    @Max(value = 1000, message = "Tổng số chỗ tối đa 1000")
    private Integer totalSlots;

    @NotNull(message = "Vui lòng nhập giá người lớn")
    @DecimalMin(value = "0", message = "Giá người lớn không được âm")
    @Digits(integer = 15, fraction = 0, message = "Giá người lớn không hợp lệ")
    private BigDecimal adultPrice;

    @NotNull(message = "Vui lòng nhập giá trẻ em")
    @DecimalMin(value = "0", message = "Giá trẻ em không được âm")
    @Digits(integer = 15, fraction = 0, message = "Giá trẻ em không hợp lệ")
    private BigDecimal childPrice;

    /** Không gửi → 0 */
    @DecimalMin(value = "0", message = "Phụ thu phòng đơn không được âm")
    @Digits(integer = 15, fraction = 0, message = "Phụ thu phòng đơn không hợp lệ")
    private BigDecimal singleRoomSupplement;

    @Size(max = 100, message = "Phương tiện tối đa 100 ký tự")
    private String transport;
}