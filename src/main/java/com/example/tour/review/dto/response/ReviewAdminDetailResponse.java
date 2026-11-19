package com.example.tour.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Popup chi tiết đánh giá (admin) */
@Getter
@Builder
public class ReviewAdminDetailResponse {
    private Integer reviewId;
    private Integer rating;
    private String comment;
    private String adminResponse;
    private boolean hidden;
    private String hiddenReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Đơn + khách
    private Integer bookingId;
    private String bookingCode;
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    // Tour
    private Integer tourId;
    private String tourName;
    private String scheduleCode;
    private LocalDate departureDate;
    private LocalDate returnDate;
}
