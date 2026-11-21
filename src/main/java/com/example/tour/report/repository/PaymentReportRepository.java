package com.example.tour.report.repository;

import com.example.tour.payment.constant.PaymentMethod;
import com.example.tour.payment.constant.TransactionStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thống kê trên bảng payments. Tiền đã nhận = giao dịch SUCCESS, theo payment_date (lúc tiền về).
 * Đơn bị huỷ + hoàn tiền sau đó vẫn tính: tiền đó ĐÃ về tài khoản (hoàn tiền làm tay bên ngoài hệ thống).
 */
@Repository
@RequiredArgsConstructor
public class PaymentReportRepository {

    private static final String SUCCESS = "p.transactionStatus = com.example.tour.payment.constant.TransactionStatus.SUCCESS";

    private final EntityManager em;

    /** Theo ngày + phương thức: [ngày, method, số GD, số tiền] */
    public List<DailyMethodRow> findDailyByMethod(LocalDateTime start, LocalDateTime end) {
        String jpql = """
                select cast(p.paymentDate as LocalDate), p.method, count(p), sum(p.amount)
                from Payment p
                where %s and p.paymentDate >= :start and p.paymentDate < :end
                group by cast(p.paymentDate as LocalDate), p.method
                """.formatted(SUCCESS);
        return em.createQuery(jpql, Object[].class)
                .setParameter("start", start).setParameter("end", end)
                .getResultList().stream()
                .map(r -> new DailyMethodRow(toLocalDate(r[0]), (PaymentMethod) r[1], toLong(r[2]), toBig(r[3])))
                .toList();
    }

    /** Theo phương thức + ngân hàng/cổng */
    public List<GatewayRow> findByGateway(LocalDateTime start, LocalDateTime end) {
        String jpql = """
                select p.method, p.gateway, count(p), sum(p.amount)
                from Payment p
                where %s and p.paymentDate >= :start and p.paymentDate < :end
                group by p.method, p.gateway
                order by sum(p.amount) desc
                """.formatted(SUCCESS);
        return em.createQuery(jpql, Object[].class)
                .setParameter("start", start).setParameter("end", end)
                .getResultList().stream()
                .map(r -> new GatewayRow((PaymentMethod) r[0], (String) r[1], toLong(r[2]), toBig(r[3])))
                .toList();
    }

    /** Lần thanh toán VNPAY tạo trong khoảng, đếm theo trạng thái: [status, số lần] */
    public List<Object[]> countVnpayAttempts(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("""
                        select p.transactionStatus, count(p)
                        from Payment p
                        where p.method = com.example.tour.payment.constant.PaymentMethod.VNPAY
                          and p.createdAt >= :start and p.createdAt < :end
                        group by p.transactionStatus
                        """, Object[].class)
                .setParameter("start", start).setParameter("end", end)
                .getResultList();
    }

    public static long count(List<Object[]> rows, TransactionStatus status) {
        return rows.stream().filter(r -> r[0] == status).mapToLong(r -> toLong(r[1])).sum();
    }

    private static long toLong(Object o) { return o == null ? 0 : ((Number) o).longValue(); }

    private static BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDateTime dt) return dt.toLocalDate();
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    public record DailyMethodRow(LocalDate date, PaymentMethod method, long transactions, BigDecimal amount) {
    }

    public record GatewayRow(PaymentMethod method, String gateway, long transactions, BigDecimal amount) {
    }
}
