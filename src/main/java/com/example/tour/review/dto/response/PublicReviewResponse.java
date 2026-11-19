package com.example.tour.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Đánh giá hiển thị công khai ở trang chi tiết tour — chỉ tên + ảnh, KHÔNG có SĐT, email, mã đơn */
@Getter
@Builder
public class PublicReviewResponse {
    private Integer reviewId;
    private String reviewerName;     // họ tên tài khoản; khách vãng lai = Họ tên trong Thông tin liên lạc
    private String reviewerAvatar;   // ảnh đại diện tài khoản; khách vãng lai = null → FE hiện hình người trống
    private Integer rating;
    private String comment;
    private String adminResponse;
    private LocalDate departureDate; // "Đã đi tour ngày ..."
    private LocalDateTime createdAt;
}
