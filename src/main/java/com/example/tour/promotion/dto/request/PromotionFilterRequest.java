package com.example.tour.promotion.dto.request;

import com.example.tour.promotion.constant.DiscountType;
import lombok.Data;

/** Bộ lọc trang quản lý: ?keyword=&discountType=&isActive=&tourId= */
@Data
public class PromotionFilterRequest {
    private String keyword;              // tìm theo mã hoặc tên
    private DiscountType discountType;
    private Boolean isActive;            // true = đã kích hoạt, false = chưa kích hoạt, null = tất cả
    private Integer tourId;              // mã áp dụng được cho tour này (kể cả mã áp dụng mọi tour)
}
