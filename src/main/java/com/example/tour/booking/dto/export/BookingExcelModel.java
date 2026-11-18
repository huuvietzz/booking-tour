package com.example.tour.booking.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.BooleanEnum;
import com.alibaba.excel.enums.poi.FillPatternTypeEnum;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 1 dòng trong file Excel "Đơn đặt tour".
 * - Thứ tự cột = index trong @ExcelProperty
 * - Ngày, trạng thái để dạng chữ tiếng Việt (đọc được ngay, không phụ thuộc định dạng ngày của máy)
 * - Tiền để dạng SỐ (BigDecimal) + dataFormat 3 = "#,##0" → Excel vẫn cộng / lọc / SUM được
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@HeadRowHeight(24)
@HeadStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 44)   // 44 = xanh nhạt (PALE_BLUE)
@HeadFontStyle(fontName = "Calibri", fontHeightInPoints = 12, bold = BooleanEnum.TRUE)
@ContentFontStyle(fontName = "Calibri", fontHeightInPoints = 11)
public class BookingExcelModel {

    @ExcelProperty(value = "STT", index = 0)
    @ColumnWidth(6)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private Integer stt;

    @ExcelProperty(value = "Mã đơn", index = 1)
    @ColumnWidth(18)
    private String bookingCode;

    @ExcelProperty(value = "Ngày đặt", index = 2)
    @ColumnWidth(18)
    private String createdAt;

    @ExcelProperty(value = "Khách hàng", index = 3)
    @ColumnWidth(24)
    private String contactName;

    @ExcelProperty(value = "Số điện thoại", index = 4)
    @ColumnWidth(14)
    private String contactPhone;

    @ExcelProperty(value = "Email", index = 5)
    @ColumnWidth(28)
    private String contactEmail;

    @ExcelProperty(value = "Tour", index = 6)
    @ColumnWidth(45)
    private String tourName;

    @ExcelProperty(value = "Mã khởi hành", index = 7)
    @ColumnWidth(22)
    private String scheduleCode;

    @ExcelProperty(value = "Ngày khởi hành", index = 8)
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String departureDate;

    @ExcelProperty(value = "Ngày về", index = 9)
    @ColumnWidth(13)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String returnDate;

    @ExcelProperty(value = "Người lớn", index = 10)
    @ColumnWidth(10)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private Integer totalAdults;

    @ExcelProperty(value = "Trẻ em", index = 11)
    @ColumnWidth(9)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private Integer totalChildren;

    @ExcelProperty(value = "Trẻ nhỏ", index = 12)
    @ColumnWidth(9)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private Integer totalToddlers;

    @ExcelProperty(value = "Tổng khách", index = 13)
    @ColumnWidth(11)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private Integer totalPeople;

    @ExcelProperty(value = "Tạm tính (₫)", index = 14)
    @ColumnWidth(15)
    @ContentStyle(dataFormat = 3)
    private BigDecimal tempAmount;

    @ExcelProperty(value = "Giảm giá (₫)", index = 15)
    @ColumnWidth(14)
    @ContentStyle(dataFormat = 3)
    private BigDecimal discountAmount;

    @ExcelProperty(value = "Mã giảm giá", index = 16)
    @ColumnWidth(14)
    private String promotionCode;

    @ExcelProperty(value = "Tổng tiền (₫)", index = 17)
    @ColumnWidth(15)
    @ContentStyle(dataFormat = 3)
    private BigDecimal totalAmount;

    @ExcelProperty(value = "Trạng thái", index = 18)
    @ColumnWidth(15)
    private String bookingStatus;

    @ExcelProperty(value = "TT Thanh toán", index = 19)
    @ColumnWidth(16)
    private String paymentStatus;

    @ExcelProperty(value = "Ghi chú", index = 20)
    @ColumnWidth(40)
    private String note;
}
