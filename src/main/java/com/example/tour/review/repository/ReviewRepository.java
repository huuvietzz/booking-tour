package com.example.tour.review.repository;

import com.example.tour.review.dto.response.TourRatingResponse;
import com.example.tour.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer>, JpaSpecificationExecutor<Review> {

    // ---------- khách ----------
    Optional<Review> findByBooking_BookingId(Integer bookingId);

    boolean existsByBooking_BookingId(Integer bookingId);

    /** Trang "Đơn của tôi": lấy review của cả trang đơn trong 1 câu (tránh N+1) */
    List<Review> findByBooking_BookingIdIn(Collection<Integer> bookingIds);

    // ---------- admin ----------
    @Override
    @EntityGraph(attributePaths = {"booking", "booking.schedule", "booking.schedule.tour"})
    Page<Review> findAll(Specification<Review> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "booking.schedule", "booking.schedule.tour"})
    Optional<Review> findWithDetailsById(Integer id);

    // tourId = null → tất cả tour; có tourId → riêng tour đó
    @Query("""
           select count(r) from Review r
           where (:tourId is null or r.booking.schedule.tour.tourId = :tourId)
             and (:hidden is null or r.hidden = :hidden)
           """)
    long countForStats(@Param("tourId") Integer tourId, @Param("hidden") Boolean hidden);

    @Query("""
           select count(r) from Review r
           where (:tourId is null or r.booking.schedule.tour.tourId = :tourId)
             and r.hidden = false and (r.adminResponse is null or trim(r.adminResponse) = '')
           """)
    long countUnreplied(@Param("tourId") Integer tourId);

    @Query("""
           select avg(r.rating) from Review r
           where (:tourId is null or r.booking.schedule.tour.tourId = :tourId) and r.hidden = false
           """)
    Double averageVisibleRating(@Param("tourId") Integer tourId);

    /** Mỗi tour có đánh giá: điểm TB + số đánh giá đang hiển thị. Điểm thấp lên đầu → admin thấy tour cần xem xét */
    @Query("""
           select new com.example.tour.review.dto.response.TourRatingResponse(
                  t.tourId, t.tourName, avg(case when r.hidden = false then r.rating end),
                  sum(case when r.hidden = false then 1L else 0L end))
           from Review r join r.booking b join b.schedule s join s.tour t
           group by t.tourId, t.tourName
           order by avg(case when r.hidden = false then r.rating end) asc, t.tourName asc
           """)
    List<TourRatingResponse> findTourRatings();

    // ---------- public (trang chi tiết tour) ----------
    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.schedule"})
    @Query(value = """
           select r from Review r
           where r.booking.schedule.tour.tourId = :tourId and r.hidden = false
             and (:rating is null or r.rating = :rating)
           """,
            countQuery = """
           select count(r) from Review r
           where r.booking.schedule.tour.tourId = :tourId and r.hidden = false
             and (:rating is null or r.rating = :rating)
           """)
    Page<Review> findPublicByTour(@Param("tourId") Integer tourId, @Param("rating") Integer rating, Pageable pageable);

    /** [rating, count] của các đánh giá đang hiển thị của tour */
    @Query("""
           select r.rating, count(r) from Review r
           where r.booking.schedule.tour.tourId = :tourId and r.hidden = false
           group by r.rating
           """)
    List<Object[]> countByRatingOfTour(@Param("tourId") Integer tourId);
}
