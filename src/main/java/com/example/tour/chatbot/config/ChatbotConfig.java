package com.example.tour.chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ChatClient.Builder do Spring AI tự tạo từ cấu hình spring.ai.openai.* (base-url trỏ sang Groq).
 * @EnableScheduling: cho ChatRateLimiter dọn bộ đếm. Project đã bật ở chỗ khác thì thêm lần nữa cũng không sao.
 */
@Configuration
@EnableScheduling
public class ChatbotConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
