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
@HeadFontStyle(fontName = "Calibri", fontHeightInPoints = 12, bold = BooleanEnum.TRUE)
@ContentFontStyle(fontName = "Calibri", fontHeightInPoints = 11)
public class TourScheduleExcelModel {
    @ExcelProperty("ID")
    @ColumnWidth(10)
    private Integer scheduleId;

    // --- Thông tin Tour liên kết ---
    @ExcelProperty("Mã Tour")
    private String tourCode;

    @ExcelProperty("Tên Tour")
    @ColumnWidth(40)
    private String tourName;

    // --- Lịch trình ---
    @ExcelProperty("Ngày Khởi Hành")
    @ColumnWidth(22)
    private String departureDate;

    @ExcelProperty("Ngày Trở Về")
    @ColumnWidth(22)
    private String returnDate;

    // --- Chỗ ---
    @ExcelProperty("Tổng Số Chỗ")
    @ColumnWidth(15)
    private Integer availableSlots;

    @ExcelProperty("Số Chỗ Đã Đặt")
    @ColumnWidth(18)
    private Integer bookedSlots;

    @ExcelProperty("Số Chỗ Còn Lại")
    @ColumnWidth(18)
    private Integer remainingSlots; // availableSlots - bookedSlots, tính lúc map

    // --- Giá ---
    @ExcelProperty("Điều Chỉnh Giá")
    private BigDecimal priceAdjustment;

    // --- Trạng thái ---
    @ExcelProperty("Trạng Thái")
    private String status;

    // --- Thời gian ---
    @ExcelProperty("Ngày Tạo")
    @ColumnWidth(25)
    private String createdAt;

    @ExcelProperty("Ngày Cập Nhật")
    @ColumnWidth(22)
    private String updatedAt;
}
