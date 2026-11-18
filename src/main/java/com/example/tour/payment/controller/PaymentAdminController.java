package com.example.tour.payment.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.payment.dto.request.PaymentFilterRequest;
import com.example.tour.payment.dto.response.PaymentAdminRowResponse;
import com.example.tour.payment.dto.response.PaymentDetailResponse;
import com.example.tour.payment.dto.response.PaymentStatsResponse;
import com.example.tour.payment.service.PaymentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentAdminService paymentAdminService;

    /**
     * GET /api/admin/payments?page=0&size=10&sort=paymentDate,desc
     *     &keyword=&transactionStatus=REVIEW&method=BANK_TRANSFER&unmatched=true
     *     &fromDate=2026-11-01&toDate=2026-11-30&minAmount=&maxAmount=
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentAdminRowResponse>>> getPayments(
            @ModelAttribute PaymentFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        Page<PaymentAdminRowResponse> data = paymentAdminService.getPayments(filter, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giao dịch thành công", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getPaymentDetail(@PathVariable Integer id) {
        PaymentDetailResponse data = paymentAdminService.getPaymentDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết giao dịch thành công", data));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PaymentStatsResponse>> getStats() {
        PaymentStatsResponse data = paymentAdminService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê thanh toán thành công", data));
    }
}
