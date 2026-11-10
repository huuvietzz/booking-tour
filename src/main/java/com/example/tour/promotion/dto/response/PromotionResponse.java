package com.example.tour.promotion.dto.response;

import com.example.tour.promotion.constant.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trả cho ADMIN. Không trả Set<Tour> (entity): Jackson sẽ đi tiếp vào các quan hệ LAZY của Tour
 * → lỗi LazyInitializationException hoặc vòng lặp JSON. Chỉ trả vài field cần hiển thị (TourItem).
 */
@Data
@Builder
public class PromotionResponse {
    private Integer promotionId;
    private String promotionCode;
    private String promotionName;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantity;
    private Integer maxUsesPerUser;
    private Integer usedCount;
    private Boolean isActive;
    private boolean appliesToAllTours;
    private List<TourItem> tours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public record TourItem(Integer tourId, String tourCode, String tourName) {}
}
