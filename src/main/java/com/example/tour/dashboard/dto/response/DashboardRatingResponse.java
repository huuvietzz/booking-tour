package com.example.tour.dashboard.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardRatingResponse {
    private long totalReviews;
    private double averageRating;
    private long previousTotalReviews;
    private double previousAverageRating;
}
