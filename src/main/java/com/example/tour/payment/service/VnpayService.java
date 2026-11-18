package com.example.tour.payment.service;

import com.example.tour.payment.dto.response.VnpayCreateResponse;
import com.example.tour.payment.dto.response.VnpayResult;

import java.util.Map;

public interface VnpayService {

    /** Tạo giao dịch PENDING + URL thanh toán VNPAY cho đơn */
    VnpayCreateResponse createPaymentUrl(String bookingCode, String clientIp);

    /** Xử lý tham số VNPAY gửi về (IPN hoặc ReturnUrl) — gọi nhiều lần vẫn an toàn */
    VnpayResult processResult(Map<String, String> params);

    /** ReturnUrl khi đã có IPN: chỉ kiểm tra chữ ký + đọc kết quả để hiển thị, KHÔNG cập nhật DB */
    VnpayResult checkReturn(Map<String, String> params);
}
