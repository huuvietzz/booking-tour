package com.example.tour.tour.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryResponse {
    private Integer itineraryId;
    private Integer dayNumber;
    private String title;
    private String description;
}