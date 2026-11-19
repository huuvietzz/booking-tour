package com.example.tour.report.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 1 dòng "Top tour doanh thu cao nhất". Constructor dùng trong JPQL select new */
@Getter
public class TopTourRevenueResponse {
    private final Integer tourId;
    private final String tourCode;
    private final String tourName;
    private final String thumbnailUrl;
    private final long bookings;
    private final long guests;
    private final BigDecimal revenue;
    @Setter
    private double share;   // % trên tổng doanh thu của kỳ — service tính sau

    public TopTourRevenueResponse(Integer tourId, String tourCode, String tourName, String thumbnailUrl,
                                  Long bookings, Long guests, BigDecimal revenue) {
        this.tourId = tourId;
        this.tourCode = tourCode;
        this.tourName = tourName;
        this.thumbnailUrl = thumbnailUrl;
        this.bookings = bookings == null ? 0 : bookings;
        this.guests = guests == null ? 0 : guests;
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
