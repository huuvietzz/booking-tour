package com.example.tour.booking.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Đơn chuyển sang HOÀN THÀNH (tour đã kết thúc) → gửi email mời đánh giá.
 * Chỉ chứa dữ liệu đã đọc sẵn (không giữ entity) vì listener chạy ở luồng khác, sau khi transaction đã đóng.
 */
@Getter
@AllArgsConstructor
public class BookingCompletedEvent {
    private final String bookingCode;
    private final String paymentToken;     // token bí mật của đơn → link đánh giá không cần đăng nhập
    private final String contactName;
    private final String contactEmail;
    private final String tourName;
    private final LocalDate departureDate;
    private final LocalDate returnDate;
}
