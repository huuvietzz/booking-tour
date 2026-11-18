package com.example.tour.payment.specification;

import com.example.tour.booking.entity.Booking;
import com.example.tour.payment.dto.request.PaymentFilterRequest;
import com.example.tour.payment.entity.Payment;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class PaymentSpecification {

    private PaymentSpecification() {}

    public static Specification<Payment> filter(PaymentFilterRequest f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (StringUtils.hasText(f.getKeyword())) {
                Join<Payment, Booking> b = root.join("booking");   // payment luôn có đơn → inner join
                String kw = "%" + f.getKeyword().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(b.get("bookingCode")), kw),
                        cb.like(cb.lower(b.get("contactName")), kw),
                        cb.like(cb.lower(b.get("contactPhone")), kw),
                        cb.like(cb.lower(root.get("referenceCode")), kw),
                        cb.like(cb.lower(root.get("transactionId")), kw)
                ));
            }
            if (f.getTransactionStatus() != null) {
                ps.add(cb.equal(root.get("transactionStatus"), f.getTransactionStatus()));
            }
            if (f.getMethod() != null) {
                ps.add(cb.equal(root.get("method"), f.getMethod()));
            }
            if (f.getFromDate() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), f.getFromDate().atStartOfDay()));
            }
            if (f.getToDate() != null) {   // < 00:00 ngày hôm sau → tính trọn ngày cuối
                ps.add(cb.lessThan(root.get("paymentDate"), f.getToDate().plusDays(1).atStartOfDay()));
            }
            if (f.getMinAmount() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("amount"), f.getMinAmount()));
            }
            if (f.getMaxAmount() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("amount"), f.getMaxAmount()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
