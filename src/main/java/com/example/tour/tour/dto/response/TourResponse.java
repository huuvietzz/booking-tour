package com.example.tour.tour.dto.response;

import com.example.tour.tour.constant.TourClass;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Chi tiết tour: có ảnh thư viện + lịch trình */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourResponse {
    private Integer tourId;
    private String tourCode;
    private String tourName;

    private Integer categoryId;
    private String categoryName;
    private Integer locationId;
    private String locationName;

    private TourClass tourClass;
    private String departureLocation;
    private String duration;
    private BigDecimal basePrice;
    private String description;

    private String thumbnailUrl;
    private List<ImageResponse> images;
    private List<ItineraryResponse> itineraries;

    private Boolean isFeatured;
    private Boolean isActive;
    private Integer viewCount;
    private BigDecimal averageRating;
    private Integer totalReviews;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}