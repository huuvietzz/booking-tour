package com.example.tour.promotion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/** Trang đặt tour bấm "Áp dụng": kiểm tra mã + tính thử số tiền giảm */
@Data
public class PromotionCheckRequest {

    @NotBlank(message = "Vui lòng nhập mã giảm giá")
    private String promotionCode;

    @NotNull(message = "Thiếu tour")
    private Integer tourId;

    /** Tạm tính trước giảm giá. Chỉ để XEM TRƯỚC — lúc tạo đơn BE phải tự tính lại từ giá của lịch */
    @NotNull(message = "Thiếu giá trị đơn hàng")
    @PositiveOrZero(message = "Giá trị đơn hàng không được âm")
    private BigDecimal orderAmount;
}
