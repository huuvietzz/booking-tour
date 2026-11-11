package com.example.tour.chatbot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ChatResponse {

    /** Câu trả lời, được dùng **đậm** và [chữ](link-nội-bộ) — FE tự escape, chỉ mở lại 2 cú pháp này */
    private String reply;

    /** Thẻ tour gợi ý (tối đa 3), LUÔN lấy từ DB, không lấy từ chữ AI viết */
    private List<TourItem> tours;

    /** Câu hỏi gợi ý tiếp (tối đa 3) */
    private List<String> suggestions;

    @Getter
    @AllArgsConstructor
    public static class TourItem {
        private Integer tourId;
        private String tourName;
        private BigDecimal basePrice;
        private String thumbnailUrl;
    }
}
