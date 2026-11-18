package com.example.tour.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cấu hình VNPAY — giá trị trong application.properties (bí mật lấy từ biến môi trường) */
@Getter
@Component
public class VnpayProperties {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    /** Sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html */
    @Value("${vnpay.pay-url}")
    private String payUrl;

    /** VNPAY đưa khách về đây (BE) sau khi thanh toán xong / huỷ */
    @Value("${vnpay.return-url}")
    private String returnUrl;

    /** Sau khi BE xử lý ReturnUrl → chuyển khách về trang này (payment.html) */
    @Value("${vnpay.frontend-result-url}")
    private String frontendResultUrl;

    /**
     * true  = ReturnUrl cũng cập nhật đơn (dùng khi CHƯA có IPN: chạy localhost, chưa gửi IPN URL cho VNPAY)
     * false = đúng tài liệu VNPAY: ReturnUrl chỉ kiểm tra chữ ký + hiển thị, chỉ IPN cập nhật đơn
     */
    @Value("${vnpay.confirm-on-return:true}")
    private boolean confirmOnReturn;
}
