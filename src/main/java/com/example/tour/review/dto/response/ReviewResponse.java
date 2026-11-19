package com.example.tour.review.dto.response;

import com.example.tour.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Đánh giá của CHÍNH khách (gắn vào đơn ở trang cá nhân) */
@Getter
@Builder
public class ReviewResponse {
    private Integer reviewId;
    private Integer rating;
    private String comment;
    private String adminResponse;
    private boolean hidden;          // khách thấy đánh giá của mình bị ẩn + lý do
    private String hiddenReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse of(Review r) {
        if (r == null) return null;
        return ReviewResponse.builder()
                .reviewId(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .adminResponse(r.getAdminResponse())
                .hidden(Boolean.TRUE.equals(r.getHidden()))
                .hiddenReason(r.getHiddenReason())
                .createdAt(r.getCreatedAt())
                // chỉ báo "đã sửa" khi sửa sau lúc tạo > 1 giây (createdAt/updatedAt sinh lệch nhau vài micro giây lúc INSERT)
                .updatedAt(edited(r) ? r.getUpdatedAt() : null)
                .build();
    }

    private static boolean edited(Review r) {
        return r.getUpdatedAt() != null && r.getCreatedAt() != null
                && r.getUpdatedAt().isAfter(r.getCreatedAt().plusSeconds(1));
    }
}
