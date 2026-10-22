package com.example.tour.tour.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.enums.BooleanEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@ColumnWidth(20)
// 1. Định dạng Font cho Hàng Tiêu đề (Header)
@HeadFontStyle(fontName = "Calibri", fontHeightInPoints = 12, bold = BooleanEnum.TRUE)
// 2. Định dạng Font cho các Hàng Dữ liệu (Content)
@ContentFontStyle(fontName = "Calibri", fontHeightInPoints = 11)
public class TourExcelModel {

    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Integer tourId;

    @ExcelProperty("Mã Tour")
    private String tourCode;

    @ExcelProperty("Tên Tour")
    @ColumnWidth(40)
    private String tourName;

    @ExcelProperty("Trạng Thái")
    private String status;

    @ExcelProperty("Loại")
    private String tourType;

    // --- Bóc tách dữ liệu ---
    @ExcelProperty("Số Ngày")
    @ColumnWidth(12)
    private Integer durationDays;

    @ExcelProperty("Số Đêm")
    @ColumnWidth(12)
    private Integer durationNights;

    // --- Bảng giá chi tiết ---
    @ExcelProperty("Giá Người Lớn")
    private BigDecimal basePrice;

    @ExcelProperty("Giá Trẻ Em")
    private BigDecimal childPrice;

    @ExcelProperty("Giá Em Bé")
    private BigDecimal infantPrice;

    // --- Chỉ số phụ ---
    @ExcelProperty("Lượt Xem")
    private Integer viewCount;

    @ExcelProperty("Số Đánh Giá")
    private Integer totalReviews;

    @ExcelProperty("Đánh Giá Trung Bình")
    private BigDecimal averageRating;

    // --- Thời gian ---
    @ExcelProperty("Ngày Tạo")
    @ColumnWidth(25)
    private String createdAt;

    @ExcelProperty("Ngày Cập Nhật")
    @ColumnWidth(22)
    private String updatedAt;
}
