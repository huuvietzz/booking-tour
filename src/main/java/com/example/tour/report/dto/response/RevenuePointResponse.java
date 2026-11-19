package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 1 cột trên biểu đồ: 1 ngày (groupBy=DAY) hoặc 1 tháng (groupBy=MONTH, date = ngày 1 của tháng) */
@Getter
@Builder
public class RevenuePointResponse {
    private LocalDate date;
    private long totalBookings;
    private long paidBookings;
    private BigDecimal revenue;
    private BigDecimal refundAmount;
}
