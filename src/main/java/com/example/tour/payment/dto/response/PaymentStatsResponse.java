package com.example.tour.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentStatsResponse {
    private long total;
    private long success;
    private long pending;     // VNPAY sau này
    private long failed;      // VNPAY sau này
    private BigDecimal successAmount;   // tổng tiền đã thu
}
