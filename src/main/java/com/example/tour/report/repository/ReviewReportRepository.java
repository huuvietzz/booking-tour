package com.example.tour.report.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Thống kê bảng reviews: chỉ đánh giá đang hiển thị (hidden = false), theo created_at */
@Repository
@RequiredArgsConstructor
public class ReviewReportRepository {

    private final EntityManager em;

    /** Số đánh giá theo từng mức sao: {5: 12, 4: 3, ...} — chỉ có các mức có đánh giá */
    public Map<Integer, Long> countByStar(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createQuery("""
                        select r.rating, count(r) from Review r
                        where r.hidden = false and r.createdAt >= :start and r.createdAt < :end
                        group by r.rating
                        """, Object[].class)
                .setParameter("start", start).setParameter("end", end)
                .getResultList();
        Map<Integer, Long> map = new HashMap<>();
        rows.forEach(r -> map.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue()));
        return map;
    }

    public long countUnreplied(LocalDateTime start, LocalDateTime end) {
        return em.createQuery("""
                        select count(r) from Review r
                        where r.hidden = false and r.createdAt >= :start and r.createdAt < :end
                          and (r.adminResponse is null or trim(r.adminResponse) = '')
                        """, Long.class)
                .setParameter("start", start).setParameter("end", end)
                .getSingleResult();
    }

    /** Theo tour, chỉ tour có ít nhất minReviews đánh giá: [tourId, mã, tên, số đánh giá, TB sao, số 1–2 sao, số 4–5 sao] */
    public List<Object[]> statsByTour(LocalDateTime start, LocalDateTime end, long minReviews) {
        return em.createQuery("""
                        select t.tourId, t.tourCode, t.tourName, count(r), avg(r.rating),
                               sum(case when r.rating <= 2 then 1 else 0 end),
                               sum(case when r.rating >= 4 then 1 else 0 end)
                        from Review r join r.booking b join b.schedule s join s.tour t
                        where r.hidden = false and r.createdAt >= :start and r.createdAt < :end
                        group by t.tourId, t.tourCode, t.tourName
                        having count(r) >= :minReviews
                        """, Object[].class)
                .setParameter("start", start).setParameter("end", end)
                .setParameter("minReviews", minReviews)
                .getResultList();
    }
}
