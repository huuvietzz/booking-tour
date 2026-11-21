package com.example.tour.report.dto.response;

import com.example.tour.payment.constant.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Tiền đã nhận theo 1 phương thức (chỉ giao dịch SUCCESS) */
@Getter
@Builder
public class PaymentMethodStatResponse {
    private PaymentMethod method;
    private long transactions;
    private BigDecimal amount;
    private double share;          // % số tiền
}
