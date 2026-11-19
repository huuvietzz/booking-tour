package com.example.tour.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Admin ẩn đánh giá — bắt buộc ghi lý do */
@Getter
@Setter
public class ReviewHideRequest {

    @NotBlank(message = "Vui lòng nhập lý do ẩn đánh giá")
    @Size(max = 255, message = "Lý do tối đa 255 ký tự")
    private String hiddenReason;
}
