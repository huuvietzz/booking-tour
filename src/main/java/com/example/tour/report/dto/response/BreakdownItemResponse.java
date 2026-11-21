package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 1 nhóm doanh thu: 1 điểm đến / 1 danh mục / 1 dòng tour */
@Getter
@Builder
public class BreakdownItemResponse {
    private String key;          // locationId / categoryId / tên enum TourClass
    private String label;        // Hạ Long / Biển đảo / PREMIUM (FE tự dịch dòng tour)
    private long bookings;
    private long guests;
    private BigDecimal revenue;
    private double share;        // % trên tổng doanh thu của nhóm
}
