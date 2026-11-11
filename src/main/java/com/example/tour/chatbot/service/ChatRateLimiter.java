package com.example.tour.chatbot.service;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn số câu hỏi / IP / phút (cửa sổ trượt 60 giây), lưu trong RAM.
 * Đủ cho 1 server. Chạy nhiều server thì phải chuyển sang Redis (RAM mỗi máy đếm riêng).
 */
@Component
public class ChatRateLimiter {

    private static final long WINDOW_MS = 60_000;

    private final int limitPerMinute;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public ChatRateLimiter(@Value("${chatbot.rate-limit-per-minute:10}") int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public void check(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (q) {   // 1 IP gửi 2 request cùng lúc → không đếm sai
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            if (q.size() >= limitPerMinute) {
                throw new AppException(ErrorCode.CHATBOT_RATE_LIMIT);
            }
            q.addLast(now);
        }
    }

    /** 5 phút dọn 1 lần: IP lâu không chat thì xoá, không để Map phình mãi */
    @Scheduled(fixedDelay = 300_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || now - last > WINDOW_MS;
            }
        });
    }
}
