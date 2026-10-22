package com.example.tour.tour.dto.response;
import com.example.tour.tour.constant.TourClass;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourCardResponse {
    private Integer tourId;
    private String thumbnailUrl;
    private TourClass tourClass;
    private String tourName;
    private String departureLocation;
    private String duration;
    private BigDecimal basePrice;
    private List<DepartureDateResponse> departureDates;
}
