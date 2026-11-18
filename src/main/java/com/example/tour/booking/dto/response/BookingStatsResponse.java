package com.example.tour.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Số liệu thanh tab trên trang admin Quản lý đặt tour */
@Getter
@Builder
public class BookingStatsResponse {
    private long total;       // Tất cả
    private long pending;     // Chờ xác nhận
    private long confirmed;   // Đã xác nhận
    private long paid;        // Đã thanh toán
    private long upcoming;    // Sắp khởi hành (đã xác nhận, đi trong N ngày tới)
    private long completed;   // Hoàn thành
    private long cancelled;   // Đã hủy
}