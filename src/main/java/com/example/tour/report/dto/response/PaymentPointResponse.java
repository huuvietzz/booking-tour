package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 1 cột biểu đồ: tiền nhận được theo từng phương thức trong ngày / tháng */
@Getter
@Builder
public class PaymentPointResponse {
    private LocalDate date;
    private BigDecimal bankTransfer;
    private BigDecimal vnpay;
    private long transactions;
}
