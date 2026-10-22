package com.example.tour.booking.dto.export;

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
public class BookingExcelModel {

    @ExcelProperty("Mã Đơn")
    @ColumnWidth(18)
    private String bookingCode;

    @ExcelProperty("Ngày Đặt")
    @ColumnWidth(20)
    private String bookingDate;

    // --- Khách hàng ---
    @ExcelProperty("Khách Hàng")
    @ColumnWidth(28)
    private String customerName;

    @ExcelProperty("Số Điện Thoại")
    @ColumnWidth(16)
    private String customerPhone;

    @ExcelProperty("Email")
    @ColumnWidth(30)
    private String customerEmail;

    // --- Chuyến đi ---
    @ExcelProperty("Mã Tour")
    @ColumnWidth(14)
    private String tourCode;

    @ExcelProperty("Tên Tour")
    @ColumnWidth(40)
    private String tourName;

    @ExcelProperty("Ngày Khởi Hành")
    @ColumnWidth(18)
    private String departureDate;

    @ExcelProperty("Ngày Trở Về")
    @ColumnWidth(18)
    private String returnDate;

    // --- Số khách ---
    @ExcelProperty("Người Lớn")
    @ColumnWidth(12)
    private Integer totalAdults;

    @ExcelProperty("Trẻ Em")
    @ColumnWidth(12)
    private Integer totalChildren;

    @ExcelProperty("Em Bé")
    @ColumnWidth(12)
    private Integer totalInfants;

    @ExcelProperty("Phụ Thu Phòng Đơn")
    private BigDecimal singleRoomSurcharge;

    @ExcelProperty("Số Phòng Đơn")
    @ColumnWidth(14)
    private Integer singleRoomCount;

    // --- Tiền ---
    @ExcelProperty("Tổng Tiền")
    private BigDecimal totalAmount;

    @ExcelProperty("Giảm Giá")
    private BigDecimal discountAmount;

    @ExcelProperty("Thành Tiền")
    private BigDecimal finalAmount;

    // --- Trạng thái ---
    @ExcelProperty("Trạng Thái Đơn")
    @ColumnWidth(18)
    private String bookingStatus;

    @ExcelProperty("Trạng Thái Thanh Toán")
    @ColumnWidth(22)
    private String paymentStatus;

    @ExcelProperty("Lý Do Hủy")
    @ColumnWidth(30)
    private String cancellationReason;
}
