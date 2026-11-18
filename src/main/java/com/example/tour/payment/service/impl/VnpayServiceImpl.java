package com.example.tour.payment.service.impl;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.constant.PaymentStatus;
import com.example.tour.booking.entity.Booking;
import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.payment.config.VnpayProperties;
import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import com.example.tour.payment.dto.response.VnpayCreateResponse;
import com.example.tour.payment.dto.response.VnpayResult;
import com.example.tour.payment.entity.Payment;
import com.example.tour.payment.repository.PaymentRepository;
import com.example.tour.payment.service.VnpayService;
import com.example.tour.payment.util.VnpayUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayServiceImpl implements VnpayService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TXN_SUFFIX = DateTimeFormatter.ofPattern("HHmmssSSS");

    private static final int HOLD_MINUTES = 60;      // giữ chỗ 1 giờ (khớp payment.html)
    private static final int VNPAY_LINK_MINUTES = 15; // link VNPAY hết hạn sau 15 phút

    private final VnpayProperties props;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final EntityManager entityManager;

    /* =========================================================
       1. TẠO URL THANH TOÁN
       ========================================================= */
    @Override
    @Transactional
    public VnpayCreateResponse createPaymentUrl(String bookingCode, String clientIp) {
        Booking booking = bookingRepository.findByBookingCodeForUpdate(bookingCode.trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getBookingStatus() != BookingStatus.PENDING || booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.BOOKING_NOT_PAYABLE);
        }

        LocalDateTime now = LocalDateTime.now(VN);
        LocalDateTime deadline = booking.getCreatedAt().plusMinutes(HOLD_MINUTES);
        if (!now.isBefore(deadline)) {
            throw new AppException(ErrorCode.PAYMENT_EXPIRED);
        }
        // link VNPAY hết hạn sau 15 phút, nhưng không vượt quá hạn giữ chỗ
        LocalDateTime expire = now.plusMinutes(VNPAY_LINK_MINUTES);
        if (expire.isAfter(deadline)) expire = deadline;

        // Mã giao dịch gửi VNPAY: phải KHÁC NHAU mỗi lần (khách bấm thanh toán lại thì tạo mã mới)
        String txnRef = booking.getBookingCode() + now.format(TXN_SUFFIX);

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .transactionId(txnRef)
                .amount(booking.getTotalAmount())
                .method(PaymentMethod.VNPAY)
                .transactionStatus(TransactionStatus.PENDING)
                .build());

        Map<String, String> p = new HashMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", props.getTmnCode());
        p.put("vnp_Amount", booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString()); // VNPAY tính x100
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_OrderInfo", "Thanh toan don " + booking.getBookingCode());   // KHÔNG dấu tiếng Việt
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", props.getReturnUrl());
        p.put("vnp_IpAddr", clientIp);
        p.put("vnp_CreateDate", now.format(VNP_TIME));
        p.put("vnp_ExpireDate", expire.format(VNP_TIME));

        String query = VnpayUtil.buildHashData(p);
        String secureHash = VnpayUtil.hmacSHA512(props.getHashSecret(), query);
        String url = props.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

        log.info("[VNPAY] Tạo giao dịch {} cho đơn {}", txnRef, booking.getBookingCode());
        return new VnpayCreateResponse(url);
    }

    /* =========================================================
       2. XỬ LÝ KẾT QUẢ (IPN + ReturnUrl dùng chung)
          Khoá dòng payment → IPN và ReturnUrl đến cùng lúc thì 1 cái xử lý, cái kia thấy "đã xử lý"
       ========================================================= */
    @Override
    @Transactional
    public VnpayResult processResult(Map<String, String> params) {
        // ĐK 1: đúng chữ ký VNPAY (không ai sửa được tham số)
        if (!VnpayUtil.verify(params, props.getHashSecret())) {
            log.warn("[VNPAY] Sai chữ ký, txnRef={}", params.get("vnp_TxnRef"));
            return new VnpayResult("97", "Invalid Checksum", null, false);
        }

        // ĐK 2: có giao dịch tương ứng
        Payment payment = paymentRepository
                .findForUpdateByMethodAndTransactionId(PaymentMethod.VNPAY, params.get("vnp_TxnRef"))
                .orElse(null);
        if (payment == null) {
            return new VnpayResult("01", "Order not found", null, false);
        }
        // lấy id từ proxy không cần query, rồi khoá dòng đơn
        Booking booking = entityManager.find(Booking.class, payment.getBooking().getBookingId(), LockModeType.PESSIMISTIC_WRITE);
        String code = booking.getBookingCode();

        // ĐK 3: đúng số tiền đã tạo
        BigDecimal paid = new BigDecimal(params.getOrDefault("vnp_Amount", "0")).movePointLeft(2);
        if (paid.compareTo(payment.getAmount()) != 0) {
            log.warn("[VNPAY] Sai số tiền txn={} ({} != {})", payment.getTransactionId(), paid, payment.getAmount());
            return new VnpayResult("04", "Invalid amount", code, false);
        }

        // ĐK 4: chưa xử lý (IPN gửi lại / ReturnUrl sau IPN)
        if (payment.getTransactionStatus() != TransactionStatus.PENDING) {
            return new VnpayResult("02", "Order already confirmed", code,
                    payment.getTransactionStatus() == TransactionStatus.SUCCESS);
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));

        payment.setReferenceCode(params.get("vnp_TransactionNo"));   // mã GD phía VNPAY
        payment.setGateway(params.get("vnp_BankCode"));              // NCB, VCB...
        payment.setPaymentDate(parsePayDate(params.get("vnp_PayDate")));

        if (!success) {
            payment.setTransactionStatus(TransactionStatus.FAILED);  // khách huỷ / thẻ lỗi...
            log.info("[VNPAY] Giao dịch {} không thành công, mã {}", payment.getTransactionId(), params.get("vnp_ResponseCode"));
            return new VnpayResult("00", "Confirm Success", code, false);
        }

        payment.setTransactionStatus(TransactionStatus.SUCCESS);
        if (booking.getBookingStatus() == BookingStatus.PENDING && booking.getPaymentStatus() == PaymentStatus.PENDING) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.PAID);            // dirty checking, không cần save
            log.info("[VNPAY] Đơn {} đã thanh toán, txn={}", code, payment.getTransactionId());
        } else {
            // đã trừ tiền nhưng đơn đã huỷ / đã trả bằng cách khác → admin hoàn tiền tay
            log.warn("[VNPAY] Đơn {} đang {}/{} nhưng vẫn nhận tiền txn={}", code,
                    booking.getBookingStatus(), booking.getPaymentStatus(), payment.getTransactionId());
        }
        return new VnpayResult("00", "Confirm Success", code, true);
    }

    /* =========================================================
       3. RETURN URL (khi IPN đã cấu hình) — chỉ đọc, không ghi
       ========================================================= */
    @Override
    @Transactional(readOnly = true)
    public VnpayResult checkReturn(Map<String, String> params) {
        if (!VnpayUtil.verify(params, props.getHashSecret())) {
            return new VnpayResult("97", "Invalid Checksum", null, false);
        }
        Payment payment = paymentRepository
                .findByMethodAndTransactionId(PaymentMethod.VNPAY, params.get("vnp_TxnRef"))
                .orElse(null);
        if (payment == null) {
            return new VnpayResult("01", "Order not found", null, false);
        }
        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        // DB do IPN cập nhật; IPN có thể đến sau vài giây → payment.html tự hỏi /payment-status cho tới khi PAID
        return new VnpayResult("00", "OK", payment.getBooking().getBookingCode(), success);
    }

    private LocalDateTime parsePayDate(String s) {
        try {
            return s == null ? LocalDateTime.now(VN) : LocalDateTime.parse(s, VNP_TIME);
        } catch (Exception e) {
            return LocalDateTime.now(VN);
        }
    }
}
