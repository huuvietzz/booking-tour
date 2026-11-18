package com.example.tour.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Kết quả xử lý tham số VNPAY gửi về (dùng chung cho IPN và ReturnUrl) */
@Getter
@AllArgsConstructor
public class VnpayResult {
    /** Mã trả cho VNPAY ở IPN: 00 ok, 01 không có đơn, 02 đã xử lý, 04 sai số tiền, 97 sai chữ ký, 99 lỗi */
    private String rspCode;
    private String message;
    /** Mã đơn để chuyển khách về payment.html (null nếu không xác định được) */
    private String bookingCode;
    /** Giao dịch đã thanh toán thành công chưa */
    private boolean success;
}
