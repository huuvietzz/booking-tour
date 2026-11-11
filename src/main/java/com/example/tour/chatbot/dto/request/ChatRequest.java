package com.example.tour.chatbot.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Body của POST /api/public/chatbot — khớp đúng cái chatbot.js đang gửi */
@Data
public class ChatRequest {

    @NotBlank(message = "Vui lòng nhập câu hỏi")
    @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
    private String message;

    /** Các tin trước đó để AI hiểu ngữ cảnh ("tour đó còn chỗ không?"). Service chỉ lấy 10 tin cuối. */
    @Valid
    @Size(max = 40)
    private List<ChatTurn> history = new ArrayList<>();

    @Data
    public static class ChatTurn {
        private String role;      // user | assistant (giá trị khác bị bỏ: chặn khách tự chèn "system")

        @Size(max = 3000)
        private String content;
    }
}
