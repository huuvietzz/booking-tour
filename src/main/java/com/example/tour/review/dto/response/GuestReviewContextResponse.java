package com.example.tour.review.dto.response;

import com.example.tour.booking.constant.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** Trang review.html (mở từ email): thông tin chuyến đi + đánh giá đã có (nếu có) */
@Getter
@Builder
public class GuestReviewContextResponse {
    private String bookingCode;
    private BookingStatus bookingStatus;   // chỉ COMPLETED mới đánh giá được
    private String reviewerName;
    private Integer tourId;
    private String tourName;
    private String thumbnailUrl;
    private String scheduleCode;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private ReviewResponse review;         // null = chưa đánh giá
}
