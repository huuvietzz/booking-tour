package com.example.tour.promotion.dto.response;

import com.example.tour.promotion.constant.DiscountType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Trả cho KHÁCH: bỏ quantity, usedCount, danh sách tour... (thông tin nội bộ) */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromotionPublicResponse {
    private String promotionCode;
    private String promotionName;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime endDate;

    private Integer maxUsesPerUser;   // mỗi khách dùng tối đa N lần (null = không giới hạn)
    private Integer userUsedCount;    // khách này đã dùng bao nhiêu lần (đơn chưa huỷ)
    private Boolean usedUp;           // true = đã dùng hết lượt → FE làm mờ
}
