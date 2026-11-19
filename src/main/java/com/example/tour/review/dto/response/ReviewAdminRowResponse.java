package com.example.tour.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 1 dòng bảng admin Quản lý đánh giá */
@Getter
@Builder
public class ReviewAdminRowResponse {
    private Integer reviewId;
    private LocalDateTime createdAt;
    private Integer rating;
    private String comment;
    private Integer bookingId;
    private String bookingCode;
    private String customerName;
    private Integer tourId;
    private String tourName;
    private boolean hidden;
    private boolean replied;
}
