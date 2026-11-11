package com.example.tour.chatbot.controller;

import com.example.tour.chatbot.dto.request.ChatRequest;
import com.example.tour.chatbot.dto.response.ChatResponse;
import com.example.tour.chatbot.service.ChatbotService;
import com.example.tour.chatbot.service.ChatRateLimiter;
import com.example.tour.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Trợ lý AI (khung chat góc phải trang khách). Nằm dưới /api/public → chưa đăng nhập vẫn chat được
 * (SecurityConfig đã permitAll "/api/public/**").
 */
@RestController
@RequestMapping("/api/public/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatRateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody @Valid ChatRequest request,
                                                          HttpServletRequest http) {
        // Mỗi câu hỏi tốn 1 lượt của hạn mức Groq → chặn 1 người spam làm hết lượt của cả web
        rateLimiter.check(http.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("OK", chatbotService.chat(request)));
    }
}
