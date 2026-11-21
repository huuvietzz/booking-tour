package com.example.tour.report.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.report.dto.response.PaymentReportResponse;
import com.example.tour.report.dto.response.RevenueReportResponse;
import com.example.tour.report.dto.response.ReviewReportResponse;
import com.example.tour.report.dto.response.TourReportResponse;
import com.example.tour.report.service.PaymentReportService;
import com.example.tour.report.service.RevenueReportService;
import com.example.tour.report.service.ReviewReportService;
import com.example.tour.report.service.TourReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Trang Báo cáo thống kê (admin/reports.html) — mỗi tab 1 API, tab nào mở mới gọi.
 * from/to dạng yyyy-MM-dd, bỏ trống = đầu tháng này → hôm nay.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final RevenueReportService revenueReportService;
    private final TourReportService tourReportService;
    private final PaymentReportService paymentReportService;
    private final ReviewReportService reviewReportService;

    // Tab "Tổng quan"
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu thành công",
                revenueReportService.getRevenueReport(from, to)));
    }

    @GetMapping("/revenue/export")
    public ResponseEntity<byte[]> exportRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] file = revenueReportService.exportRevenueReport(from, to);
        String name = from != null && to != null ? "bao-cao-doanh-thu_" + from + "_" + to + ".xlsx" : "bao-cao-doanh-thu.xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(file);
    }

    // Tab "Tour & điểm đến": lấp đầy (theo ngày khởi hành) + doanh thu theo điểm đến / danh mục / dòng tour
    @GetMapping("/tours")
    public ResponseEntity<ApiResponse<TourReportResponse>> getTourReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo tour thành công",
                tourReportService.getTourReport(from, to)));
    }

    // Tab "Thanh toán": tiền đã nhận theo phương thức / ngân hàng / thời gian + tỷ lệ thành công VNPAY
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentReportResponse>> getPaymentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo thanh toán thành công",
                paymentReportService.getPaymentReport(from, to)));
    }

    // Tab "Đánh giá": đánh giá đang hiển thị, theo ngày khách gửi
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewReportResponse>> getReviewReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo đánh giá thành công",
                reviewReportService.getReviewReport(from, to)));
    }
}
