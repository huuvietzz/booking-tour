package com.example.tour.report.dto.response;

import com.example.tour.report.constant.ReportGroupBy;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Tab "Thanh toán" — tính theo THỜI ĐIỂM TIỀN VỀ (payments.payment_date), chỉ giao dịch thành công */
@Getter
@Builder
public class PaymentReportResponse {
    private LocalDate from;
    private LocalDate to;
    private ReportGroupBy groupBy;

    private BigDecimal totalAmount;
    private long totalTransactions;
    private List<PaymentMethodStatResponse> methods;     // luôn đủ 2 phương thức (kể cả = 0)
    private List<PaymentPointResponse> series;
    private List<PaymentGatewayStatResponse> gateways;

    // Lần thanh toán VNPAY khách mở trong khoảng (theo created_at): thành công / thất bại / đang chờ
    private long vnpayAttempts;
    private long vnpaySuccess;
    private long vnpayFailed;
    private long vnpayPending;
    private double vnpaySuccessRate;   // success / (success + failed), không tính đang chờ
}
