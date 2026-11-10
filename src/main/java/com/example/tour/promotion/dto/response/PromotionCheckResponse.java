package com.example.tour.promotion.dto.response;

import com.example.tour.promotion.constant.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromotionCheckResponse {
    private String promotionCode;
    private String promotionName;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal orderAmount;      // trước giảm
    private BigDecimal discountAmount;   // số tiền được giảm
    private BigDecimal finalAmount;      // sau giảm
}
