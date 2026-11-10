package com.example.tour.promotion.dto.response;

import com.example.tour.promotion.constant.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Trả cho KHÁCH: bỏ quantity, usedCount, danh sách tour... (thông tin nội bộ) */
@Data
@Builder
public class PromotionPublicResponse {
    private String promotionCode;
    private String promotionName;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime endDate;
}
