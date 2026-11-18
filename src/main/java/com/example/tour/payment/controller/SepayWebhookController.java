package com.example.tour.payment.controller;

import com.example.tour.payment.dto.request.SepayWebhookRequest;
import com.example.tour.payment.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class SepayWebhookController {

    private final SepayWebhookService sepayWebhookService;

    @Value("${sepay.webhook.api-key}")
    private String apiKey;

    @PostMapping("/sepay")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SepayWebhookRequest req) {

        // Xác thực: header dạng "Apikey <KEY>"
        if (!isValidKey(authorization)) {
            log.warn("[SePay] Sai API key, từ chối webhook id={}", req.getId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            sepayWebhookService.handle(req);
        } catch (DataIntegrityViolationException dup) {
            // 2 request trùng id đến cùng lúc -> unique key chặn, coi như đã xử lý
            log.info("[SePay] Trùng giao dịch id={} (unique key)", req.getId());
        }
        // Trả đúng format SePay yêu cầu, nếu không SePay sẽ coi là lỗi và gửi lại
        return ResponseEntity.ok(Map.of("success", true));
    }

    private boolean isValidKey(String authorization) {
        if (authorization == null || !authorization.startsWith("Apikey ")) return false;
        String received = authorization.substring("Apikey ".length()).trim();
        // so sánh constant-time, tránh timing attack
        return MessageDigest.isEqual(
                received.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8));
    }
}
