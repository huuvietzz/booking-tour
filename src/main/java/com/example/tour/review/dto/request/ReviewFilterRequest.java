package com.example.tour.review.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Bộ lọc trang admin Quản lý đánh giá */
@Getter
@Setter
public class ReviewFilterRequest {

    /** Nội dung bình luận, tên khách, mã đơn, tên tour */
    private String keyword;

    /** 1..5 */
    private Integer rating;

    /** true = đã ẩn, false = đang hiển thị */
    private Boolean hidden;

    /** true = đã phản hồi, false = chưa phản hồi */
    private Boolean replied;

    private Integer tourId;

    /** Ngày gửi đánh giá */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
}
