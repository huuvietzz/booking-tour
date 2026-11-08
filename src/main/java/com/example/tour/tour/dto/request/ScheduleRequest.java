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

    @NotNull(message = "Vui lòng nhập tổng số chỗ")
    @Min(value = 1, message = "Tổng số chỗ phải từ 1 trở lên")
    @Max(value = 1000, message = "Tổng số chỗ tối đa 1000")
    private Integer totalSlots;

    // adultPrice lấy từ basePrice trong bảng tour
    // Cách làm của bạn đơn giản hơn, phù hợp nếu đồ án không cần giá theo mùa.
    // Có một điểm phải xử lý cẩn thận: khi base_price thay đổi, không được cập nhật giá
    // của các lịch đã có khách đặt.

    // Đây cũng là lý do vẫn nên giữ cột adult_price trong bảng schedules. Cột này đóng
    // vai trò “ảnh chụp” giá của lịch: lịch chưa có khách đặt thì giá chạy theo tour,
    // lịch đã có khách đặt thì giá được giữ nguyên.
    /*
    * Kết quả:

        Lịch	                               Khi tour đổi giá
        Chưa khởi hành, chưa có booking	       Giá người lớn đổi theo
        Đã có booking (kể cả đơn đã huỷ)	   Giữ nguyên giá cũ
        Đã khởi hành	                       Giữ nguyên, vì là dữ liệu lịch sử
    * */
    @NotNull(message = "Vui lòng nhập giá trẻ em")
    @DecimalMin(value = "0", message = "Giá trẻ em không được âm")
    @Digits(integer = 15, fraction = 0, message = "Giá trẻ em không hợp lệ")
    private BigDecimal childPrice;

    @NotNull(message = "Vui lòng nhập giá trẻ nhỏ")
    @DecimalMin(value = "0", message = "Giá trẻ nhỏ không được âm")
    @Digits(integer = 15, fraction = 0, message = "Giá trẻ nhỏ không hợp lệ")
    private BigDecimal toddlerPrice;

    @NotNull(message = "Vui lòng nhập phụ thu phòng đơn")
    @DecimalMin(value = "0", message = "Phụ thu phòng đơn không được âm")
    @Digits(integer = 15, fraction = 0, message = "Phụ thu phòng đơn không hợp lệ")
    private BigDecimal singleRoomSupplement;

    @Size(max = 100, message = "Phương tiện tối đa 100 ký tự")
    private String transport;
}