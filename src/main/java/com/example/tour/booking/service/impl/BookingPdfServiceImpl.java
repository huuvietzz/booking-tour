package com.example.tour.booking.service.impl;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.ParticipantType;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.dto.response.BookingResponse;
import com.example.tour.booking.service.BookingPdfService;
import com.example.tour.booking.service.BookingService;
import com.example.tour.common.constant.Gender;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Phiếu xác nhận đặt tour (A4 dọc) bằng iText 8.
 *
 * Bố cục: Đầu phiếu (công ty | mã đơn, trạng thái) → 1. Liên hệ → 2. Chuyến đi
 *        → 3. Hành khách → 4. Thanh toán → Ghi chú / lưu ý → Chữ ký → Chân trang (giờ xuất, số trang)
 *
 * Tiếng Việt: font mặc định của iText KHÔNG có dấu → nhúng Roboto (resources/fonts) với IDENTITY_H.
 */
@Service
@RequiredArgsConstructor
public class BookingPdfServiceImpl implements BookingPdfService {

    private final BookingService bookingService;

    // Thông tin công ty: đổi trong application.properties, không cần sửa code
    @Value("${app.company.name:VietTrip}")
    private String companyName;
    @Value("${app.company.address:Số 123 Đường Cầu Diễn, Phú Diễn, Bắc Từ Liêm, Hà Nội}")
    private String companyAddress;
    @Value("${app.company.hotline:+84 123 456 789}")
    private String companyHotline;
    @Value("${app.company.email:lienhe@viettrip.vn}")
    private String companyEmail;

    // ===== MÀU (giống trang admin) =====
    private static final Color PRIMARY    = new DeviceRgb(0x41, 0x54, 0xF1);
    private static final Color DARK       = new DeviceRgb(0x21, 0x25, 0x29);
    private static final Color MUTED      = new DeviceRgb(0x6C, 0x75, 0x7D);
    private static final Color BORDER     = new DeviceRgb(0xDE, 0xE2, 0xE6);
    private static final Color HEAD_BG    = new DeviceRgb(0xEE, 0xF0, 0xFD);
    private static final Color ZEBRA_BG   = new DeviceRgb(0xF8, 0xF9, 0xFA);
    private static final Color TOTAL_RED  = new DeviceRgb(0xD3, 0x2F, 0x2F);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // Đọc file font 1 lần rồi giữ byte[]; PdfFont thì phải tạo mới cho MỖI file PDF (không dùng chung giữa các document)
    private static volatile byte[] regularFont, boldFont, italicFont;

    /** 3 font của 1 lần xuất */
    private record Fonts(PdfFont regular, PdfFont bold, PdfFont italic) {}

    @Override
    public byte[] exportBookingPdf(Integer bookingId) {
        // Dùng lại hàm admin đã có: nạp sẵn lịch, tour, mã KM, hành khách. Không có đơn → BOOKING_NOT_FOUND
        BookingResponse b = bookingService.getBookingByIdForAdmin(bookingId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        pdf.getDocumentInfo().setTitle("Phiếu xác nhận đặt tour " + b.getBookingCode());
        pdf.getDocumentInfo().setAuthor(companyName);

        // immediateFlush = false: giữ các trang lại để cuối cùng ghi "Trang x/y" (lúc đó mới biết tổng số trang)
        Document doc = new Document(pdf, PageSize.A4, false);
        doc.setMargins(36, 40, 48, 40);

        Fonts f = new Fonts(font(FontKind.REGULAR), font(FontKind.BOLD), font(FontKind.ITALIC));
        doc.setFont(f.regular()).setFontSize(10).setFontColor(DARK);

        addHeader(doc, f, b);
        addContact(doc, f, b);
        addTrip(doc, f, b);
        addParticipants(doc, f, b);
        addPayment(doc, f, b);
        addNotes(doc, f, b);
        addSignatures(doc, f);
        addFooter(doc, pdf, f);

        doc.close();
        return out.toByteArray();
    }

    // =====================================================================
    // ĐẦU PHIẾU: trái = công ty, phải = tên phiếu + mã đơn + trạng thái
    // =====================================================================
    private void addHeader(Document doc, Fonts f, BookingResponse b) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{55, 45})).useAllAvailableWidth();

        Cell left = noBorder(new Cell())
                .add(new Paragraph(companyName.toUpperCase()).setFont(f.bold()).setFontSize(20).setFontColor(PRIMARY)
                        .setMarginBottom(2))
                .add(small(companyAddress, f))
                .add(small("Hotline: " + companyHotline, f))
                .add(small("Email: " + companyEmail, f));

        Cell right = noBorder(new Cell()).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("PHIẾU XÁC NHẬN ĐẶT TOUR").setFont(f.bold()).setFontSize(14).setMarginBottom(4))
                .add(new Paragraph().add(new Text("Mã đơn: ").setFontColor(MUTED))
                        .add(new Text(nz(b.getBookingCode())).setFont(f.bold()).setFontColor(PRIMARY)).setMargin(0))
                .add(new Paragraph().add(new Text("Ngày đặt: ").setFontColor(MUTED))
                        .add(new Text(fmtDateTime(b.getCreatedAt()))).setMargin(0))
                .add(new Paragraph()
                        .add(badge(bookingStatusText(b.getBookingStatus()), bookingStatusColor(b.getBookingStatus()), f))
                        .add(new Text("  "))
                        .add(badge(paymentStatusText(b.getPaymentStatus()), paymentStatusColor(b.getPaymentStatus()), f))
                        .setMarginTop(6).setMarginBottom(0));

        t.addCell(left).addCell(right);
        doc.add(t);

        SolidLine line = new SolidLine(1.2f);
        line.setColor(PRIMARY);
        doc.add(new LineSeparator(line).setMarginTop(8).setMarginBottom(4));
    }

    // =====================================================================
    // 1. THÔNG TIN LIÊN HỆ
    // =====================================================================
    private void addContact(Document doc, Fonts f, BookingResponse b) {
        doc.add(sectionTitle("1. THÔNG TIN LIÊN HỆ", f));
        Table t = infoTable();
        infoRow(t, f, "Họ tên", b.getContactName(), "Điện thoại", b.getContactPhone());
        infoRow(t, f, "Email", b.getContactEmail(), "Địa chỉ", dash(b.getContactAddress()));
        doc.add(t);
    }

    // =====================================================================
    // 2. THÔNG TIN CHUYẾN ĐI
    // =====================================================================
    private void addTrip(Document doc, Fonts f, BookingResponse b) {
        doc.add(sectionTitle("2. THÔNG TIN CHUYẾN ĐI", f));
        Table t = infoTable();

        // Tên tour dài → chiếm cả hàng (gộp 3 ô bên phải)
        t.addCell(label("Tour", f));
        t.addCell(noBorder(new Cell(1, 3)).add(new Paragraph(nz(b.getTourName())).setFont(f.bold())));

        infoRow(t, f, "Mã khởi hành", b.getScheduleCode(), "Phương tiện", dash(b.getTransport()));
        infoRow(t, f, "Ngày khởi hành", fmtDateWithDay(b.getDepartureDate()), "Ngày về", fmtDateWithDay(b.getReturnDate()));
        infoRow(t, f, "Số khách", guestSummary(b), "Phòng đơn",
                n(b.getSingleRoomCount()) > 0 ? n(b.getSingleRoomCount()) + " phòng" : "Không");
        doc.add(t);
    }

    // =====================================================================
    // 3. DANH SÁCH HÀNH KHÁCH (dòng tiêu đề lặp lại nếu bảng sang trang mới)
    // =====================================================================
    private void addParticipants(Document doc, Fonts f, BookingResponse b) {
        List<BookingResponse.Participant> list = b.getParticipants() == null ? List.of() : b.getParticipants();
        doc.add(sectionTitle("3. DANH SÁCH HÀNH KHÁCH (" + list.size() + ")", f));

        Table t = new Table(UnitValue.createPercentArray(new float[]{6, 30, 14, 10, 14, 16, 10})).useAllAvailableWidth();
        for (String h : new String[]{"STT", "Họ tên", "Loại khách", "Giới tính", "Ngày sinh", "Điện thoại", "Phòng đơn"}) {
            t.addHeaderCell(headCell(h, f, TextAlignment.CENTER));
        }

        int i = 0;
        for (BookingResponse.Participant p : list) {
            boolean zebra = i % 2 == 1;
            boolean adult = p.getParticipantType() == ParticipantType.ADULT;
            t.addCell(bodyCell(String.valueOf(++i), TextAlignment.CENTER, zebra));
            t.addCell(bodyCell(nz(p.getFullName()), TextAlignment.LEFT, zebra).setFont(f.bold()));
            t.addCell(bodyCell(participantTypeText(p.getParticipantType()), TextAlignment.CENTER, zebra));
            t.addCell(bodyCell(genderText(p.getGender()), TextAlignment.CENTER, zebra));
            t.addCell(bodyCell(fmtDate(p.getDateOfBirth()), TextAlignment.CENTER, zebra));
            // Chỉ người lớn có SĐT
            t.addCell(bodyCell(adult ? dash(p.getPhoneNumber()) : "—", TextAlignment.CENTER, zebra));
            t.addCell(bodyCell(p.isSingleRoom() ? "Có" : "", TextAlignment.CENTER, zebra));
        }
        if (list.isEmpty()) {   // 1 ô trải hết 7 cột
            t.addCell(new Cell(1, 7).add(new Paragraph("Chưa có thông tin hành khách").setFont(f.italic()).setMargin(0))
                    .setTextAlignment(TextAlignment.CENTER).setBorder(new SolidBorder(BORDER, 0.6f)).setPadding(5));
        }
        doc.add(t);
    }

    // =====================================================================
    // 4. CHI TIẾT THANH TOÁN: bảng từng khoản + khối tổng bên phải
    // =====================================================================
    private void addPayment(Document doc, Fonts f, BookingResponse b) {
        doc.add(sectionTitle("4. CHI TIẾT THANH TOÁN", f));

        Table t = new Table(UnitValue.createPercentArray(new float[]{46, 10, 22, 22})).useAllAvailableWidth();
        t.addHeaderCell(headCell("Khoản", f, TextAlignment.LEFT));
        t.addHeaderCell(headCell("SL", f, TextAlignment.CENTER));
        t.addHeaderCell(headCell("Đơn giá", f, TextAlignment.RIGHT));
        t.addHeaderCell(headCell("Thành tiền", f, TextAlignment.RIGHT));

        int[] row = {0};
        priceRow(t, row, "Người lớn", n(b.getTotalAdults()), b.getAdultPrice());
        priceRow(t, row, "Trẻ em", n(b.getTotalChildren()), b.getChildPrice());
        priceRow(t, row, "Trẻ nhỏ", n(b.getTotalToddlers()), b.getToddlerPrice());
        priceRow(t, row, "Phụ thu phòng đơn", n(b.getSingleRoomCount()), b.getSingleRoomSupplement());
        doc.add(t);

        // Khối tổng: nửa bên phải, không viền
        BigDecimal discount = b.getDiscountAmount() == null ? BigDecimal.ZERO : b.getDiscountAmount();
        Table sum = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(50)).setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginTop(6);
        sumRow(sum, "Tạm tính", money(b.getTempAmount()), f, false);
        sumRow(sum, b.getPromotionCode() != null ? "Giảm giá (" + b.getPromotionCode() + ")" : "Giảm giá",
                discount.signum() > 0 ? "- " + money(discount) : money(BigDecimal.ZERO), f, false);
        sumRow(sum, "TỔNG TIỀN", money(b.getTotalAmount()), f, true);
        doc.add(sum);
    }

    // =====================================================================
    // GHI CHÚ + LƯU Ý
    // =====================================================================
    private void addNotes(Document doc, Fonts f, BookingResponse b) {
        if (b.getNote() != null && !b.getNote().isBlank()) {
            doc.add(new Paragraph().add(new Text("Ghi chú của khách: ").setFont(f.bold()))
                    .add(new Text(b.getNote())).setMarginTop(12).setMarginBottom(0));
        }
        doc.add(new Paragraph("Lưu ý: Quý khách vui lòng có mặt tại điểm hẹn trước giờ khởi hành 30 phút, "
                + "mang theo CCCD / giấy khai sinh (trẻ em) và phiếu xác nhận này. "
                + "Mọi thắc mắc xin liên hệ hotline " + companyHotline + ".")
                .setFont(f.italic()).setFontSize(9).setFontColor(MUTED).setMarginTop(8));
    }

    // =====================================================================
    // CHỮ KÝ
    // =====================================================================
    private void addSignatures(Document doc, Fonts f) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth().setMarginTop(18);
        for (String who : new String[]{"KHÁCH HÀNG", "ĐẠI DIỆN " + companyName.toUpperCase()}) {
            t.addCell(noBorder(new Cell()).setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph(who).setFont(f.bold()).setMargin(0))
                    .add(new Paragraph("(Ký, ghi rõ họ tên)").setFont(f.italic()).setFontSize(9).setFontColor(MUTED)
                            .setMargin(0).setMarginBottom(56)));   // chừa chỗ ký
        }
        // Không để chữ ký bị tách: nếu cuối trang không đủ chỗ thì đẩy cả khối sang trang sau
        t.setKeepTogether(true);
        doc.add(t);
    }

    // =====================================================================
    // CHÂN TRANG: giờ xuất + "Trang x/y" ở MỌI trang (ghi sau khi đã biết tổng số trang)
    // =====================================================================
    private void addFooter(Document doc, PdfDocument pdf, Fonts f) {
        int total = pdf.getNumberOfPages();
        String printed = "Xuất lúc " + LocalDateTime.now().format(DATE_TIME);
        float width = pdf.getDefaultPageSize().getWidth();
        float y = 24;
        for (int i = 1; i <= total; i++) {
            doc.showTextAligned(new Paragraph(printed).setFont(f.italic()).setFontSize(8).setFontColor(MUTED),
                    40, y, i, TextAlignment.LEFT, VerticalAlignment.BOTTOM, 0);
            doc.showTextAligned(new Paragraph("Trang " + i + "/" + total).setFontSize(8).setFontColor(MUTED),
                    width - 40, y, i, TextAlignment.RIGHT, VerticalAlignment.BOTTOM, 0);
        }
    }

    // =====================================================================
    // KHỐI DỰNG SẴN
    // =====================================================================
    private Paragraph sectionTitle(String text, Fonts f) {
        return new Paragraph(text).setFont(f.bold()).setFontSize(11).setFontColor(PRIMARY)
                .setMarginTop(14).setMarginBottom(6)
                .setBorderBottom(new SolidBorder(BORDER, 0.8f)).setPaddingBottom(3);
    }

    /** Bảng 4 cột: nhãn | giá trị | nhãn | giá trị */
    private Table infoTable() {
        return new Table(UnitValue.createPercentArray(new float[]{17, 33, 17, 33})).useAllAvailableWidth();
    }

    private void infoRow(Table t, Fonts f, String l1, String v1, String l2, String v2) {
        t.addCell(label(l1, f)).addCell(value(v1))
                .addCell(label(l2, f)).addCell(value(v2));
    }

    private Cell label(String text, Fonts f) {
        return noBorder(new Cell()).add(new Paragraph(text).setFontColor(MUTED).setMargin(0)).setPaddingTop(2).setPaddingBottom(2);
    }

    private Cell value(String text) {
        return noBorder(new Cell()).add(new Paragraph(nz(text)).setMargin(0)).setPaddingTop(2).setPaddingBottom(2);
    }

    private Cell headCell(String text, Fonts f, TextAlignment align) {
        return new Cell().add(new Paragraph(text).setFont(f.bold()).setFontSize(9).setMargin(0))
                .setTextAlignment(align).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(HEAD_BG).setBorder(new SolidBorder(BORDER, 0.6f)).setPadding(5);
    }

    private Cell bodyCell(String text, TextAlignment align, boolean zebra) {
        Cell c = new Cell().add(new Paragraph(text).setMargin(0)).setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(BORDER, 0.6f)).setPadding(5);
        return zebra ? c.setBackgroundColor(ZEBRA_BG) : c;
    }

    /** 1 dòng tiền; số lượng 0 thì bỏ (vd không có trẻ nhỏ) */
    private void priceRow(Table t, int[] row, String name, int qty, BigDecimal unit) {
        if (qty <= 0) return;
        BigDecimal unitPrice = unit == null ? BigDecimal.ZERO : unit;
        boolean zebra = row[0]++ % 2 == 1;
        t.addCell(bodyCell(name, TextAlignment.LEFT, zebra));
        t.addCell(bodyCell(String.valueOf(qty), TextAlignment.CENTER, zebra));
        t.addCell(bodyCell(money(unitPrice), TextAlignment.RIGHT, zebra));
        t.addCell(bodyCell(money(unitPrice.multiply(BigDecimal.valueOf(qty))), TextAlignment.RIGHT, zebra));
    }

    private void sumRow(Table t, String name, String amount, Fonts f, boolean total) {
        Paragraph l = new Paragraph(name).setMargin(0);
        Paragraph r = new Paragraph(amount).setMargin(0);
        if (total) {
            l.setFont(f.bold()).setFontSize(12);
            r.setFont(f.bold()).setFontSize(13).setFontColor(TOTAL_RED);
        } else {
            l.setFontColor(MUTED);
        }
        Cell lc = noBorder(new Cell()).add(l).setPaddingTop(3).setPaddingBottom(3);
        Cell rc = noBorder(new Cell()).add(r).setTextAlignment(TextAlignment.RIGHT).setPaddingTop(3).setPaddingBottom(3);
        if (total) {   // kẻ 1 đường phía trên dòng tổng
            lc.setBorderTop(new SolidBorder(BORDER, 1f)).setPaddingTop(6);
            rc.setBorderTop(new SolidBorder(BORDER, 1f)).setPaddingTop(6);
        }
        t.addCell(lc).addCell(rc);
    }

    private Text badge(String text, Color bg, Fonts f) {
        return new Text(" " + text + " ").setFont(f.bold()).setFontSize(9)
                .setFontColor(ColorConstants.WHITE).setBackgroundColor(bg);
    }

    private Paragraph small(String text, Fonts f) {
        return new Paragraph(text).setFontSize(9).setFontColor(MUTED).setMargin(0);
    }

    private static Cell noBorder(Cell c) {
        return c.setBorder(Border.NO_BORDER).setPadding(0);
    }

    // =====================================================================
    // ĐỊNH DẠNG + NHÃN TIẾNG VIỆT
    // =====================================================================
    private static String money(BigDecimal v) {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));   // 5.000.000
        return nf.format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    private static String fmtDate(LocalDate d) { return d == null ? "—" : d.format(DATE); }

    private static String fmtDateTime(LocalDateTime d) { return d == null ? "—" : d.format(DATE_TIME); }

    private static String fmtDateWithDay(LocalDate d) {
        if (d == null) return "—";
        String day = d.getDayOfWeek() == DayOfWeek.SUNDAY ? "CN" : "T" + (d.getDayOfWeek().getValue() + 1);
        return day + ", " + d.format(DATE);
    }

    private static String guestSummary(BookingResponse b) {
        StringBuilder sb = new StringBuilder();
        if (n(b.getTotalAdults()) > 0) sb.append(b.getTotalAdults()).append(" người lớn");
        if (n(b.getTotalChildren()) > 0) sb.append(sb.isEmpty() ? "" : ", ").append(b.getTotalChildren()).append(" trẻ em");
        if (n(b.getTotalToddlers()) > 0) sb.append(sb.isEmpty() ? "" : ", ").append(b.getTotalToddlers()).append(" trẻ nhỏ");
        return sb.isEmpty() ? "—" : sb.toString();
    }

    private static String bookingStatusText(BookingStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case PENDING -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
        };
    }

    private static Color bookingStatusColor(BookingStatus s) {
        if (s == null) return MUTED;
        return switch (s) {
            case PENDING -> new DeviceRgb(0xE8, 0x89, 0x0C);
            case CONFIRMED -> new DeviceRgb(0x29, 0x48, 0xB8);
            case CANCELLED -> new DeviceRgb(0xC1, 0x2A, 0x3A);
            case COMPLETED -> new DeviceRgb(0x1E, 0x7E, 0x43);
        };
    }

    private static String paymentStatusText(PaymentStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case PENDING -> "Chưa thanh toán";
            case PAID -> "Đã thanh toán";
            case REFUNDED -> "Hoàn tiền";
        };
    }

    private static Color paymentStatusColor(PaymentStatus s) {
        if (s == null) return MUTED;
        return switch (s) {
            case PENDING -> new DeviceRgb(0xA8, 0x6A, 0x00);
            case PAID -> new DeviceRgb(0x1E, 0x7E, 0x43);
            case REFUNDED -> new DeviceRgb(0x5F, 0x6B, 0x7A);
        };
    }

    private static String participantTypeText(ParticipantType t) {
        if (t == null) return "—";
        return switch (t) {
            case ADULT -> "Người lớn";
            case CHILD -> "Trẻ em";
            case TODDLER -> "Trẻ nhỏ";
        };
    }

    /** Dùng name() thay vì switch: enum Gender của bạn có thêm giá trị khác cũng không lỗi biên dịch */
    private static String genderText(Gender g) {
        if (g == null) return "—";
        return switch (g.name()) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            default -> "Khác";
        };
    }

    private static int n(Integer v) { return v == null ? 0 : v; }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String dash(String s) { return s == null || s.isBlank() ? "—" : s; }

    // =====================================================================
    // FONT TIẾNG VIỆT (resources/fonts/*.ttf)
    // =====================================================================
    private enum FontKind { REGULAR, BOLD, ITALIC }

    private static PdfFont font(FontKind kind) {
        byte[] bytes = switch (kind) {
            case REGULAR -> regularFont != null ? regularFont : (regularFont = read("fonts/Roboto-Regular.ttf"));
            case BOLD -> boldFont != null ? boldFont : (boldFont = read("fonts/Roboto-Bold.ttf"));
            case ITALIC -> italicFont != null ? italicFont : (italicFont = read("fonts/Roboto-Italic.ttf"));
        };
        try {
            // IDENTITY_H: mã hoá Unicode → hiển thị đủ dấu tiếng Việt; PREFER_EMBEDDED: nhúng font vào file
            // → mở trên máy không cài Roboto vẫn đúng chữ
            return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được font PDF", e);
        }
    }

    private static byte[] read(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Thiếu file font " + path + " trong src/main/resources", e);
        }
    }
}
