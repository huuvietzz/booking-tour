package com.example.tour.tour.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    private String categoryName;

    @NotBlank(message = "Slug không được để trống")
    private String slug;

    private String description;

    @Min(value = 1, message = "Thứ tự bắt đầu từ 1")
    private Integer displayOrder;

    private Boolean isActive;
}
