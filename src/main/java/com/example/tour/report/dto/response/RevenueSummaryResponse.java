package com.example.tour.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Số liệu tổng của 1 khoảng thời gian (tính theo NGÀY ĐẶT của đơn).
 *   revenue        = tổng tiền đơn ĐÃ THANH TOÁN (đơn đã hoàn tiền không tính)
 *   pending*       = đơn chưa huỷ, chưa thanh toán (đang giữ chỗ)
 *   cancelled*     = đơn đã huỷ (gồm cả đơn huỷ đã hoàn tiền)
 *   refund*        = đơn đã huỷ sau khi khách trả tiền → phải hoàn
 */
@Getter
@Builder
public class RevenueSummaryResponse {
    private long totalBookings;
    private long paidBookings;
    private BigDecimal revenue;
    private long guests;                 // số khách của các đơn đã thanh toán
    private BigDecimal averageOrderValue; // revenue / paidBookings
    private long pendingBookings;
    private BigDecimal pendingAmount;
    private long cancelledBookings;
    private long refundedBookings;
    private BigDecimal refundAmount;
    private double cancelRate;           // % đơn bị huỷ trên tổng đơn
}
