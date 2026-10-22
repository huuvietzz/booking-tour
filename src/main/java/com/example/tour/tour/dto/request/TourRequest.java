package com.example.tour.tour.dto.request;

import com.example.tour.tour.constant.TourClass;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Phần "data" (JSON) của request multipart thêm / sửa tour.
 * Ảnh KHÔNG nằm ở đây: thumbnail và galleryImages là các part file riêng.
 */
@Getter
@Setter
@NoArgsConstructor
public class TourRequest {

    @NotNull(message = "Vui lòng chọn danh mục")
    private Integer categoryId;

    @NotNull(message = "Vui lòng chọn địa điểm")
    private Integer locationId;

    @NotBlank(message = "Tên tour không được để trống")
    @Size(max = 255, message = "Tên tour quá dài")
    private String tourName;

    @NotNull(message = "Vui lòng chọn dòng tour")
    private TourClass tourClass;

    @NotBlank(message = "Điểm khởi hành không được để trống")
    @Size(max = 100, message = "Điểm khởi hành quá dài")
    private String departureLocation;

    @NotBlank(message = "Thời gian tour không được để trống")
    @Size(max = 20, message = "Thời gian tour tối đa 20 ký tự (VD: 4N3Đ)")
    private String duration;

    @NotNull(message = "Giá cơ bản không được để trống")
    @DecimalMin(value = "0", message = "Giá không được âm")
    @Digits(integer = 15, fraction = 0, message = "Giá không hợp lệ")
    private BigDecimal basePrice;

    private String description;

    @NotNull(message = "Vui lòng chọn có nổi bật hay không")
    private Boolean isFeatured;

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean isActive;

    /**
     * Lịch trình theo ngày. Thứ tự trong danh sách = ngày 1, 2, 3... (server tự đánh số)
     * KHI SỬA: null → giữ nguyên lịch trình cũ; [] → xoá hết.
     */
    @Valid
    private List<ItineraryRequest> itineraries;

    /**
     * CHỈ DÙNG KHI SỬA: id các ảnh thư viện cũ muốn GIỮ LẠI, đúng thứ tự hiển thị.
     * null  → giữ nguyên toàn bộ ảnh cũ
     * []    → xoá hết ảnh cũ
     * Ảnh mới (galleryImages) được thêm vào sau các ảnh giữ lại.
     */
    private List<Integer> keepImageIds;
}