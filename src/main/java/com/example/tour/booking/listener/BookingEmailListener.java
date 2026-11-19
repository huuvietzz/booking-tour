package com.example.tour.booking.listener;

import com.example.tour.booking.constant.BookingConstants;
import com.example.tour.booking.constant.ParticipantType;
import com.example.tour.booking.dto.response.BookingResponse;
import com.example.tour.booking.event.BookingCreatedEvent;
import com.example.tour.common.service.EmailService;
import com.example.tour.common.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Gửi email "Xác nhận giữ chỗ" cho người liên hệ sau khi đặt tour.
 *
 * - AFTER_COMMIT: chỉ gửi khi đơn ĐÃ LƯU THẬT vào DB. Đặt lỗi / rollback (hết chỗ, mã KM hết lượt...) → không gửi.
 * - @Async: gửi mail chạy luồng riêng → API đặt tour trả về ngay, khách sang trang thanh toán không phải chờ SMTP.
 *   Gửi lỗi chỉ ghi log, KHÔNG làm hỏng đơn đã đặt.
 * - Mã QR: tạo ảnh PNG bằng ZXing rồi đính kèm INLINE (cid:bookingQr). Không dùng data:image/base64 vì Gmail chặn,
 *   không dùng web tạo QR bên ngoài vì sẽ lộ link đơn cho bên thứ ba.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEmailListener {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.company.hotline:+84 123 456 789}")
    private String hotline;

    /** Tên ảnh QR đính kèm inline, HTML dùng <img src="cid:bookingQr"> */
    private static final String QR_CID = "bookingQr";

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        BookingResponse b = event.getBooking();
        try {
            LocalDateTime created = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now();
            // Hạn thanh toán do BE tính sẵn (createdAt + BookingConstants.HOLD_SECONDS) → khớp job tự huỷ + payment.html
            LocalDateTime deadline = b.getPaymentDeadline() != null
                    ? b.getPaymentDeadline()
                    : LocalDateTime.now().plusSeconds(BookingConstants.HOLD_SECONDS);
            String subject = "Thông tin đặt chỗ tour trọn gói tại VietTrip - Mã đặt chỗ " + b.getBookingCode();

            // QR = link trang đơn hàng / thanh toán: khách quét bằng điện thoại là mở ngay đơn của mình
            byte[] qrPng = QrCodeGenerator.png(bookingUrl(b), 300);
            emailService.sendHtmlEmail(b.getContactEmail(), subject, buildHtml(b, deadline), Map.of(QR_CID, qrPng));
        } catch (Exception e) {
            log.error("Gửi email đặt tour {} thất bại: {}", b.getBookingCode(), e.getMessage(), e);
        }
    }

    // =====================================================================
    // NỘI DUNG EMAIL (HTML, style inline vì Gmail / Outlook bỏ thẻ <style>)
    // Mọi dữ liệu khách nhập đều qua esc() → chống chèn HTML vào email
    // =====================================================================
    /** Link trang đơn hàng / thanh toán — kèm token để mở được trên mọi thiết bị, không cần đăng nhập */
    private String bookingUrl(BookingResponse b) {
        String url = baseUrl + "/user/payment.html?code=" + URLEncoder.encode(b.getBookingCode(), StandardCharsets.UTF_8);
        if (b.getPaymentToken() != null) {
            url += "&token=" + URLEncoder.encode(b.getPaymentToken(), StandardCharsets.UTF_8);
        }
        return url;
    }

    private String buildHtml(BookingResponse b, LocalDateTime deadline) {
        String payUrl = bookingUrl(b);

        StringBuilder pax = new StringBuilder();
        int i = 1;
        if (b.getParticipants() != null) {
            for (BookingResponse.Participant p : b.getParticipants()) {
                pax.append("<tr>")
                        .append(td(String.valueOf(i++)))
                        .append(td(esc(p.getFullName())))
                        .append(td(typeText(p.getParticipantType())))
                        .append(td(p.getDateOfBirth() != null ? p.getDateOfBirth().format(DATE) : ""))
                        .append(td(p.isSingleRoom() ? "Có" : ""))
                        .append("</tr>");
            }
        }

        BigDecimal discount = b.getDiscountAmount() == null ? BigDecimal.ZERO : b.getDiscountAmount();

        return """
            <div style="background:#f4f6fb;padding:24px 12px;font-family:Arial,Helvetica,sans-serif;color:#212529">
              <div style="max-width:640px;margin:0 auto">
                <div style="text-align:center;padding:4px 0 18px;font-size:30px;font-weight:bold;color:#0e8ba8;letter-spacing:.5px">VietTrip
                  <div style="font-size:12px;font-weight:normal;color:#6c757d;letter-spacing:2px">KHÁM PHÁ VIỆT NAM</div>
                </div>

                <!-- Thẻ chính: nền xanh chuyển tím (Gmail không hỗ trợ gradient thì dùng màu nền background-color) -->
                <div style="background-color:#0b5f8a;background-image:linear-gradient(135deg,#0e8ba8 0%%,#0b4f9c 55%%,#6a2c8e 100%%);
                            border-radius:20px;padding:28px 30px;color:#fff">
                  <div style="font-size:22px;font-weight:bold;line-height:1.35;margin-bottom:14px">
                    Thông tin đặt chỗ tour trọn gói của Quý khách đã sẵn sàng!</div>
                  <div style="font-size:14px;line-height:1.6">
                    Kính gửi Quý khách <b>%s</b><br>
                    VietTrip cảm ơn Quý khách đã lựa chọn VietTrip đồng hành cùng chuyến đi sắp tới.
                    Quý khách vui lòng xem kỹ thông tin chi tiết ngay sau đây.
                  </div>

                  <table style="width:100%%;margin-top:20px;border-collapse:separate;background:rgba(255,255,255,.12);
                                border:1px solid rgba(255,255,255,.35);border-radius:14px" cellpadding="0" cellspacing="0">
                    <tr>
                      <td style="padding:20px 22px;vertical-align:top">
                        <div style="font-size:13px;letter-spacing:1px;opacity:.9">&#127915; MÃ ĐẶT CHỖ</div>
                        <div style="font-size:28px;font-weight:bold;margin:6px 0 8px;letter-spacing:1px">%s</div>
                        <div style="font-size:12px;opacity:.85;line-height:1.5">Hãy chuẩn bị Mã đặt chỗ khi cần liên hệ bộ phận hỗ trợ VietTrip.</div>
                        <a href="%s" style="display:inline-block;margin-top:16px;background:#ffffff;color:#0b4f9c;text-decoration:none;
                                  font-weight:bold;font-size:14px;padding:11px 22px;border-radius:10px">Quản lý đặt chỗ của tôi</a>
                      </td>
                      <td style="padding:20px 22px 20px 0;width:130px;vertical-align:middle;text-align:right">
                        <img src="cid:bookingQr" width="120" height="120" alt="QR đơn %s"
                             style="display:block;margin-left:auto;background:#fff;border-radius:10px;padding:6px">
                      </td>
                    </tr>
                  </table>
                </div>

                <div style="background:#fff;border-radius:20px;margin-top:18px;padding:24px 28px;font-size:14px;line-height:1.6">
                  <p style="margin-top:0">Chỗ của Quý khách <b>đã được giữ</b>.
                     Vui lòng thanh toán trước <b style="color:#d32f2f">%s</b>, quá hạn đơn sẽ tự động huỷ.</p>

                  <table style="width:100%%;border-collapse:collapse;margin:16px 0">
                    %s%s%s%s%s%s
                  </table>

                  <div style="font-weight:bold;color:#0e8ba8;margin:18px 0 6px">Danh sách hành khách</div>
                  <table style="width:100%%;border-collapse:collapse;font-size:13px">
                    <tr style="background:#eef6f8">%s%s%s%s%s</tr>
                    %s
                  </table>

                  <table style="width:100%%;border-collapse:collapse;margin-top:16px">
                    %s%s
                    <tr><td style="padding:8px 0;font-size:16px;font-weight:bold">Cần thanh toán</td>
                        <td style="padding:8px 0;text-align:right;font-size:18px;font-weight:bold;color:#d32f2f">%s</td></tr>
                  </table>

                  <div style="margin:22px 0 0;padding:14px 18px;background:#fff5f5;border:1px solid #fde0e0;
                              border-left:4px solid #e0252b;border-radius:10px;font-size:14px;color:#212529">
                    Quý khách vui lòng hoàn tất thanh toán trước thời hạn trên để tránh booking bị hủy tự động.
                  </div>

                  <div style="text-align:center;margin:22px 0 10px">
                    <a href="%s" style="background:#d32f2f;color:#fff;text-decoration:none;padding:12px 32px;border-radius:999px;font-weight:bold;display:inline-block">
                      Thanh toán ngay</a>
                  </div>
                  <p style="font-size:13px;color:#6c757d;text-align:center">Quý khách cần hỗ trợ, hãy gọi hotline <b>%s</b>.</p>
                </div>
                <div style="color:#6c757d;font-size:12px;padding:14px 28px;text-align:center">
                  Email được gửi tự động, vui lòng không trả lời email này.
                </div>
              </div>
            </div>
            """.formatted(
                esc(b.getContactName()),
                esc(b.getBookingCode()),
                payUrl,
                esc(b.getBookingCode()),
                deadline.format(DATE_TIME),
                row("Mã đơn", "<b style=\"color:#f26b1d\">" + esc(b.getBookingCode()) + "</b>"),
                row("Tour", "<b>" + esc(b.getTourName()) + "</b>"),
                row("Mã khởi hành", esc(b.getScheduleCode())),
                row("Ngày đi - về", date(b.getDepartureDate()) + " → " + date(b.getReturnDate())),
                row("Số khách", guestSummary(b)),
                row("Hạn thanh toán", "<b style=\"color:#d32f2f\">" + deadline.format(DATE_TIME) + "</b>"),
                th("#"), th("Họ tên"), th("Loại khách"), th("Ngày sinh"), th("Phòng đơn"),
                pax,
                row("Tạm tính", money(b.getTempAmount())),
                row(b.getPromotionCode() != null ? "Giảm giá (" + esc(b.getPromotionCode()) + ")" : "Giảm giá",
                        discount.signum() > 0 ? "- " + money(discount) : money(BigDecimal.ZERO)),
                money(b.getTotalAmount()),
                payUrl,
                esc(hotline));
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:6px 0;color:#6c757d;width:40%\">" + label + "</td>"
                + "<td style=\"padding:6px 0;text-align:right\">" + value + "</td></tr>";
    }

    private static String th(String s) {
        return "<th style=\"padding:8px;text-align:left;border-bottom:1px solid #dee2e6\">" + s + "</th>";
    }

    private static String td(String s) {
        return "<td style=\"padding:8px;border-bottom:1px solid #f1f3f5\">" + s + "</td>";
    }

    private static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }

    private static String date(LocalDate d) { return d == null ? "—" : d.format(DATE); }

    private static String money(BigDecimal v) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(v == null ? BigDecimal.ZERO : v) + " ₫";
    }

    private static String typeText(ParticipantType t) {
        if (t == null) return "";
        return switch (t) {
            case ADULT -> "Người lớn";
            case CHILD -> "Trẻ em";
            case TODDLER -> "Trẻ nhỏ";
        };
    }

    private static String guestSummary(BookingResponse b) {
        StringBuilder sb = new StringBuilder();
        if (n(b.getTotalAdults()) > 0) sb.append(b.getTotalAdults()).append(" người lớn");
        if (n(b.getTotalChildren()) > 0) sb.append(sb.isEmpty() ? "" : ", ").append(b.getTotalChildren()).append(" trẻ em");
        if (n(b.getTotalToddlers()) > 0) sb.append(sb.isEmpty() ? "" : ", ").append(b.getTotalToddlers()).append(" trẻ nhỏ");
        return sb.toString();
    }

    private static int n(Integer v) { return v == null ? 0 : v; }
}