package com.example.tour.promotion.entity;

import com.example.tour.common.entity.BaseEntity;
import com.example.tour.promotion.constant.DiscountType;
import com.example.tour.tour.entity.Tour;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Integer promotionId;

    @Column(name = "promotion_code", nullable = false, unique = true, length = 50)
    private String promotionCode;

    @Column(name = "promotion_name", nullable = false)
    private String promotionName;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 15, scale = 0)
    private BigDecimal maxDiscountAmount;

    @Builder.Default
    @Column(name = "min_order_amount", precision = 15, scale = 0)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /** Tổng số lần sử dụng tối đa. NULL = không giới hạn */
    @Column(name = "quantity")
    private Integer quantity;

    @Builder.Default
    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser = 1;

    @Builder.Default
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    // Promotion là owning side của bảng trung gian promotion_tours
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "promotion_tours",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "tour_id"))
    private Set<Tour> tours = new HashSet<>();
}


