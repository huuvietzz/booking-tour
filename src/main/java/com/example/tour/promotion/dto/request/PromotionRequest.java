package com.example.tour.promotion.dto.request;

import com.example.tour.promotion.constant.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Admin tạo / sửa mã.
 *  - Nhận tourIds (Set<Integer>) chứ KHÔNG nhận Set<Tour>: client chỉ cần gửi id, nhận entity thì client gửi được
 *    cả object Tour tuỳ ý và Jackson phải dựng entity từ JSON → dễ lỗi, dễ ghi đè dữ liệu tour.
 *  - KHÔNG có usedCount: đây là bộ đếm hệ thống tự tăng khi có đơn dùng mã, admin không được sửa tay.
 *  - @Data không kèm @Builder: Jackson cần constructor rỗng, @Builder đứng một mình làm mất constructor đó.
 */
@Data
public class PromotionRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    @Size(max = 50, message = "Mã khuyến mãi tối đa 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã chỉ gồm chữ không dấu, số, - và _")
    private String promotionCode;

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 255, message = "Tên khuyến mãi tối đa 255 ký tự")
    private String promotionName;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn loại giảm giá")
    private DiscountType discountType;

    /** PERCENT: 1 - 100 (%), FIXED: số tiền giảm (VNĐ) */
    @NotNull(message = "Giá trị giảm không được để trống")
    @Positive(message = "Giá trị giảm phải lớn hơn 0")
    @Digits(integer = 15, fraction = 0, message = "Giá trị giảm phải là số nguyên")
    private BigDecimal discountValue;

    /** Chỉ dùng cho PERCENT: giảm tối đa bao nhiêu tiền. NULL = không giới hạn */
    @Positive(message = "Giảm tối đa phải lớn hơn 0")
    @Digits(integer = 15, fraction = 0, message = "Giảm tối đa phải là số nguyên")
    private BigDecimal maxDiscountAmount;

    @PositiveOrZero(message = "Giá trị đơn tối thiểu không được âm")
    @Digits(integer = 15, fraction = 0, message = "Giá trị đơn tối thiểu phải là số nguyên")
    private BigDecimal minOrderAmount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    /** Tổng số lượt dùng. NULL = không giới hạn */
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Số lần dùng tối đa mỗi khách không được để trống")
    @Positive(message = "Số lần dùng tối đa mỗi khách phải lớn hơn 0")
    private Integer maxUsesPerUser = 1;

    private Boolean isActive = true;

    /** Rỗng = áp dụng cho TẤT CẢ tour */
    private Set<Integer> tourIds = new HashSet<>();
}
