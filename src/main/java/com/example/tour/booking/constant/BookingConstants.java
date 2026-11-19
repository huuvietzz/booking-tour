package com.example.tour.booking.constant;

/** Hằng số dùng chung cho đơn đặt tour — 1 chỗ sửa, mọi nơi khớp nhau */
public final class BookingConstants {

    private BookingConstants() {}

    /**
     * Thời gian giữ chỗ chờ thanh toán (giây).
     * Dùng ở: BookingExpireJob (tự huỷ), BookingResponse.paymentDeadline (payment.html + email), VNPAY.
     * Test nhanh: 30. Chạy thật: 60 * 60.
     */
    public static final long HOLD_SECONDS = 60 * 60;
}
