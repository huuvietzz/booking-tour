package com.example.tour.review.listener;

import com.example.tour.booking.event.BookingCompletedEvent;
import com.example.tour.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Email "Mời đánh giá chuyến đi" — gửi cho MỌI khách (có tài khoản hay vãng lai) khi đơn chuyển sang Hoàn thành.
 * - AFTER_COMMIT: chỉ gửi khi trạng thái Hoàn thành đã lưu thật.
 * - @Async: admin bấm "Hoàn thành" không phải chờ SMTP. Gửi lỗi chỉ ghi log.
 * - 5 ngôi sao trong email là 5 link: bấm sao nào → mở trang đánh giá chọn sẵn số sao đó.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewInviteEmailListener {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.company.hotline:+84 123 456 789}")
    private String hotline;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCompleted(BookingCompletedEvent e) {
        if (e.getPaymentToken() == null || e.getContactEmail() == null) {
            log.warn("Không gửi mời đánh giá đơn {}: thiếu token hoặc email", e.getBookingCode());
            return;
        }
        try {
            String subject = "VietTrip - Chuyến đi \"" + e.getTourName() + "\" của Quý khách thế nào?";
            emailService.sendHtmlEmail(e.getContactEmail(), subject, buildHtml(e), Map.of());
        } catch (Exception ex) {
            log.error("Gửi email mời đánh giá đơn {} thất bại: {}", e.getBookingCode(), ex.getMessage(), ex);
        }
    }

    private String reviewUrl(BookingCompletedEvent e, Integer rating) {
        String url = baseUrl + "/user/review.html?code=" + enc(e.getBookingCode()) + "&token=" + enc(e.getPaymentToken());
        return rating == null ? url : url + "&rating=" + rating;
    }

    private String buildHtml(BookingCompletedEvent e) {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append("<a href=\"").append(reviewUrl(e, i)).append("\" title=\"").append(i).append(" sao\"")
                    .append(" style=\"font-size:36px;color:#f5a524;text-decoration:none;padding:0 4px\">&#9733;</a>");
        }
        return """
            <div style="background:#f4f6fb;padding:24px 12px;font-family:Arial,Helvetica,sans-serif;color:#212529">
              <div style="max-width:600px;margin:0 auto">
                <div style="text-align:center;padding:4px 0 18px;font-size:30px;font-weight:bold;color:#0e8ba8">VietTrip
                  <div style="font-size:12px;font-weight:normal;color:#6c757d;letter-spacing:2px">KHÁM PHÁ VIỆT NAM</div>
                </div>
                <div style="background:#fff;border-radius:20px;padding:28px 30px;text-align:center">
                  <div style="font-size:21px;font-weight:bold;margin-bottom:12px">Cảm ơn Quý khách đã đồng hành cùng VietTrip!</div>
                  <div style="font-size:14px;line-height:1.6;color:#495057">
                    Kính gửi Quý khách <b>%s</b>,<br>
                    Chuyến đi <b>%s</b> (%s → %s) đã kết thúc.
                    Quý khách thấy chuyến đi thế nào? Đánh giá của Quý khách giúp VietTrip phục vụ tốt hơn
                    và giúp những du khách khác lựa chọn tour phù hợp.
                  </div>
                  <div style="margin:22px 0 6px">%s</div>
                  <div style="font-size:12px;color:#8a909c;margin-bottom:20px">Bấm vào số sao để đánh giá</div>
                  <a href="%s" style="display:inline-block;background:#0e8ba8;color:#fff;text-decoration:none;font-weight:bold;
                            padding:12px 32px;border-radius:999px">Viết đánh giá</a>
                  <div style="font-size:12px;color:#8a909c;margin-top:18px">Mã đặt chỗ: <b>%s</b></div>
                </div>
                <div style="color:#6c757d;font-size:12px;padding:14px 20px;text-align:center">
                  Cần hỗ trợ, vui lòng gọi hotline <b>%s</b>. Email được gửi tự động, vui lòng không trả lời.
                </div>
              </div>
            </div>
            """.formatted(
                esc(e.getContactName()),
                esc(e.getTourName()),
                date(e.getDepartureDate()), date(e.getReturnDate()),
                stars,
                reviewUrl(e, null),
                esc(e.getBookingCode()),
                esc(hotline));
    }

    private static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String date(LocalDate d) { return d == null ? "—" : d.format(DATE); }
}
