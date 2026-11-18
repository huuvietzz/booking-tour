package com.example.tour.booking.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Trả về cho payment.html hỏi định kỳ (public, không cần đăng nhập).
 * CHỈ chứa trạng thái — không có tên, SĐT, email, số tiền... vì mã đơn có thể đoán được.
 */
@Getter
@Builder
public class BookingPaymentStatusResponse {
    private String bookingCode;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
}
