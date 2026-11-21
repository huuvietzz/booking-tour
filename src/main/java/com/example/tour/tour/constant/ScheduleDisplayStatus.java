package com.example.tour.tour.constant;

import com.example.tour.tour.entity.Schedule;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;

/**
 * Trạng thái HIỂN THỊ của lịch khởi hành = cột status (AVAILABLE / FULL / CANCELLED) + so ngày với hôm nay.
 *
 * Vì sao không thêm DEPARTED / FINISHED vào ScheduleStatus (cột trong DB):
 *   2 trạng thái này đổi theo NGÀY, không theo thao tác của ai. Lưu vào DB thì phải có job chạy mỗi ngày để cập nhật,
 *   job lỗi / chạy trễ là dữ liệu sai. Tính từ departure_date / return_date thì luôn đúng, không cần job.
 *
 * Thứ tự ưu tiên: CANCELLED > FINISHED > DEPARTED > AVAILABLE / FULL
 */
public enum ScheduleDisplayStatus {
    AVAILABLE,   // còn chỗ, chưa khởi hành
    FULL,        // hết chỗ, chưa khởi hành
    DEPARTED,    // đã khởi hành, đang trong chuyến: ngày đi ≤ hôm nay ≤ ngày về
    FINISHED,    // đã kết thúc: ngày về < hôm nay
    CANCELLED;   // đã huỷ

    /** Trạng thái hiển thị của 1 lịch — dùng trong ScheduleServiceImpl.toResponse */
    public static ScheduleDisplayStatus of(Schedule s, LocalDate today) {
        if (s.getStatus() == ScheduleStatus.CANCELLED) return CANCELLED;
        if (s.getReturnDate().isBefore(today)) return FINISHED;
        if (!s.getDepartureDate().isAfter(today)) return DEPARTED;
        return s.getStatus() == ScheduleStatus.FULL ? FULL : AVAILABLE;
    }

    /** Điều kiện WHERE tương ứng — dùng trong ScheduleSpecification khi lọc theo trạng thái */
    public Predicate toPredicate(Root<Schedule> root, CriteriaBuilder cb, LocalDate today) {
        Path<ScheduleStatus> status = root.get("status");
        Path<LocalDate> departure = root.get("departureDate");
        Path<LocalDate> returnDate = root.get("returnDate");
        Predicate notCancelled = cb.notEqual(status, ScheduleStatus.CANCELLED);

        return switch (this) {
            case CANCELLED -> cb.equal(status, ScheduleStatus.CANCELLED);
            case FINISHED -> cb.and(notCancelled, cb.lessThan(returnDate, today));
            case DEPARTED -> cb.and(notCancelled,
                    cb.lessThanOrEqualTo(departure, today), cb.greaterThanOrEqualTo(returnDate, today));
            case AVAILABLE -> cb.and(cb.equal(status, ScheduleStatus.AVAILABLE), cb.greaterThan(departure, today));
            case FULL -> cb.and(cb.equal(status, ScheduleStatus.FULL), cb.greaterThan(departure, today));
        };
    }
}
