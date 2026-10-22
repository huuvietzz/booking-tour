package com.example.tour.tour.dto.response;

import lombok.*;

import java.math.BigDecimal;

/** Một dòng trong bảng quản lý tour (admin) */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourAdminRowResponse {
    private Integer tourId;
    private String thumbnailUrl;
    private String tourCode;
    private String tourName;
    private String categoryName;
    private String locationName;
    private BigDecimal basePrice;
    private Boolean isActive;
}