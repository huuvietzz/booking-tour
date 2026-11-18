package com.example.tour.payment.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.payment.config.VnpayProperties;
import com.example.tour.payment.dto.response.VnpayCreateResponse;
import com.example.tour.payment.dto.response.VnpayResult;
import com.example.tour.payment.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VnpayController {

    private final VnpayService vnpayService;
    private final VnpayProperties props;

    /** 1. payment.html bấm "Thanh toán VNPAY" → trả URL VNPAY để chuyển trang */
    @PostMapping("/public/bookings/{code}/vnpay")
    public ResponseEntity<ApiResponse<VnpayCreateResponse>> createPayment(@PathVariable String code,
                                                                          HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo giao dịch VNPAY thành công",
                vnpayService.createPaymentUrl(code, clientIp(request))));
    }

    /**
     * 2. IPN: server VNPAY gọi thẳng về (GET), phải trả JSON {"RspCode","Message"}.
     * Cần URL public (ngrok) + đăng ký IPN URL với VNPAY.
     */
    @GetMapping("/payments/vnpay/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        try {
            VnpayResult r = vnpayService.processResult(params);
            return Map.of("RspCode", r.getRspCode(), "Message", r.getMessage());
        } catch (Exception e) {
            log.error("[VNPAY] Lỗi xử lý IPN", e);
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }

    /**
     * 3. ReturnUrl: VNPAY đưa trình duyệt khách về đây.
     *  - vnpay.confirm-on-return=true : xử lý giống IPN (dùng khi chưa có IPN, chạy localhost)
     *  - vnpay.confirm-on-return=false: đúng tài liệu VNPAY, chỉ kiểm tra chữ ký, IPN mới cập nhật đơn
     * Sau đó chuyển khách về payment.html?code=...&vnpay=success|failed|invalid
     */
    @GetMapping("/payments/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        String status;
        String code = null;
        try {
            VnpayResult r = props.isConfirmOnReturn()
                    ? vnpayService.processResult(params)
                    : vnpayService.checkReturn(params);
            code = r.getBookingCode();
            status = "97".equals(r.getRspCode()) ? "invalid" : r.isSuccess() ? "success" : "failed";
        } catch (Exception e) {
            log.error("[VNPAY] Lỗi xử lý ReturnUrl", e);
            status = "failed";
        }
        URI target = UriComponentsBuilder.fromUriString(props.getFrontendResultUrl())
                .queryParam("code", code == null ? "" : code)
                .queryParam("vnpay", status)
                .build().encode().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");   // qua ngrok / proxy
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;    // localhost IPv6
    }
}
