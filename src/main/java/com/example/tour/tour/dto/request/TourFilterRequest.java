package com.example.tour.tour.dto.request;

import com.example.tour.tour.constant.TourClass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Tham số lọc danh sách tour (query string). Trường nào null → bỏ qua. */
@Getter
@Setter
@NoArgsConstructor
public class TourFilterRequest {
    private String keyword;        // tìm theo tên hoặc mã tour
    private Integer categoryId;
    private Integer locationId;
    private TourClass tourClass;
    private Boolean isFeatured;
    private Boolean isActive;      // chỉ admin dùng; phía khách luôn = true
    private BigDecimal minPrice;   // cả admin và khách.
    private BigDecimal maxPrice;  // cả admin và khách.
}