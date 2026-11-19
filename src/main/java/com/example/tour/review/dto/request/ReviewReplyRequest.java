package com.example.tour.review.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Admin phản hồi đánh giá — để trống = xoá phản hồi */
@Getter
@Setter
public class ReviewReplyRequest {

    @Size(max = 1000, message = "Phản hồi tối đa 1000 ký tự")
    private String adminResponse;
}
