package com.example.tour.tour.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    private Integer imageId;
    private String imageUrl;
    private Integer displayOrder;
}