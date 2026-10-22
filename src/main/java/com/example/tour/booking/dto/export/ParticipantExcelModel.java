package com.example.tour.booking.dto.export;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.enums.BooleanEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ColumnWidth(20)
@HeadFontStyle(fontName = "Calibri", fontHeightInPoints = 12, bold = BooleanEnum.TRUE)
@ContentFontStyle(fontName = "Calibri", fontHeightInPoints = 11)
public class ParticipantExcelModel {

    @ExcelProperty("STT")
    @ColumnWidth(8)
    private Integer index;

    @ExcelProperty("Họ Tên")
    @ColumnWidth(28)
    private String fullName;

    @ExcelProperty("Ngày Sinh")
    @ColumnWidth(16)
    private String dateOfBirth;

    @ExcelProperty("Giới Tính")
    @ColumnWidth(12)
    private String gender;

    @ExcelProperty("Quốc Tịch")
    @ColumnWidth(16)
    private String nationality;

    @ExcelProperty("Loại Khách")
    @ColumnWidth(14)
    private String participantType;

    @ExcelProperty("Loại Giấy Tờ")
    @ColumnWidth(14)
    private String identityType;

    @ExcelProperty("Số Giấy Tờ")
    @ColumnWidth(22)
    private String identityNumber;

    @ExcelProperty("Ngày Hết Hạn")
    @ColumnWidth(16)
    private String identityExpiryDate;

    @ExcelProperty("Nơi Cấp")
    @ColumnWidth(25)
    private String identityIssuePlace;

    @ExcelProperty("Đại Diện Đoàn")
    @ColumnWidth(16)
    private String primary;

    @ExcelProperty("Phòng Đơn")
    @ColumnWidth(12)
    private String singleRoom;   // "X" hoặc ""

    @ExcelProperty("Trạng Thái Hồ Sơ")
    @ColumnWidth(20)
    private String documentStatus;
}