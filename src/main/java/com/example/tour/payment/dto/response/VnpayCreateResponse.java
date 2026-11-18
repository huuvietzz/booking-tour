package com.example.tour.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VnpayCreateResponse {
    /** URL trang thanh toán VNPAY (đã ký) — FE chỉ cần location.href = paymentUrl */
    private String paymentUrl;
}
