package com.example.tour.review.service.impl;

import com.example.tour.booking.constant.BookingStatus;
import com.example.tour.booking.entity.Booking;
import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.review.dto.request.ReviewFilterRequest;
import com.example.tour.review.dto.request.ReviewRequest;
import com.example.tour.review.dto.response.*;
import com.example.tour.review.entity.Review;
import com.example.tour.review.repository.ReviewRepository;
import com.example.tour.review.service.ReviewService;
import com.example.tour.review.specification.ReviewSpecification;
import com.example.tour.tour.entity.Schedule;
import com.example.tour.tour.repository.TourRepository;
import com.example.tour.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt", "rating", "rating");

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;

    // =====================================================================
    // KHÁCH
    // =====================================================================
    @Override
    @Transactional
    public ReviewResponse createMyReview(Integer userId, Integer bookingId, ReviewRequest request) {
        Booking booking = myCompletedBooking(userId, bookingId);
        if (reviewRepository.existsByBooking_BookingId(bookingId)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        try {
            Review review = reviewRepository.saveAndFlush(Review.builder()
                    .booking(booking)
                    .rating(request.getRating())
                    .comment(trimToNull(request.getComment()))
                    .build());
            syncTourRating(tourIdOf(booking));
            return ReviewResponse.of(review);
        } catch (DataIntegrityViolationException e) {
            // bấm "Gửi" 2 lần cùng lúc → unique booking_id chặn lần thứ 2
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public ReviewResponse updateMyReview(Integer userId, Integer bookingId, ReviewRequest request) {
        Booking booking = myCompletedBooking(userId, bookingId);
        Review review = reviewRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        review.setRating(request.getRating());
        review.setComment(trimToNull(request.getComment()));
        // Đánh giá đang bị ẩn thì sửa xong VẪN ẩn: admin xem lại rồi mới hiện (tránh sửa nhẹ để lách)
        reviewRepository.flush();   // để updatedAt mới có trong response
        syncTourRating(tourIdOf(booking));
        return ReviewResponse.of(review);
    }

    /** Đơn của chính khách + đã hoàn thành. Đơn người khác → "không tìm thấy" (không lộ là có tồn tại) */
    private Booking myCompletedBooking(Integer userId, Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> b.getUser() != null && userId != null && b.getUser().getUserId() == userId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        return booking;
    }

    // =====================================================================
    // ADMIN
    // =====================================================================
    @Override
    public Page<ReviewAdminRowResponse> getReviewsForAdmin(ReviewFilterRequest filter, int page, int size, String sort) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), buildSort(sort));
        return reviewRepository.findAll(ReviewSpecification.adminFilter(filter), pageable).map(this::toRow);
    }

    @Override
    public ReviewAdminDetailResponse getReviewDetail(Integer reviewId) {
        return toDetail(find(reviewId));
    }

    @Override
    @Transactional
    public ReviewAdminDetailResponse hideReview(Integer reviewId, String reason) {
        Review r = find(reviewId);
        r.setHidden(true);
        r.setHiddenReason(reason.trim());
        syncTourRating(tourIdOf(r.getBooking()));
        return toDetail(r);
    }

    @Override
    @Transactional
    public ReviewAdminDetailResponse unhideReview(Integer reviewId) {
        Review r = find(reviewId);
        r.setHidden(false);
        r.setHiddenReason(null);
        syncTourRating(tourIdOf(r.getBooking()));
        return toDetail(r);
    }

    @Override
    @Transactional
    public ReviewAdminDetailResponse replyReview(Integer reviewId, String adminResponse) {
        Review r = find(reviewId);
        r.setAdminResponse(trimToNull(adminResponse));   // để trống = xoá phản hồi
        return toDetail(r);
    }

    @Override
    public List<TourRatingResponse> getTourRatings() {
        return reviewRepository.findTourRatings();
    }

    // =====================================================================
    // PUBLIC (trang chi tiết tour) — chỉ đánh giá đang hiển thị
    // =====================================================================
    @Override
    public Page<PublicReviewResponse> getTourReviews(Integer tourId, Integer rating, Pageable pageable) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findPublicByTour(tourId, rating, p).map(r -> {
            Booking b = r.getBooking();
            User u = b.getUser();   // null = khách vãng lai (đánh giá qua link email)
            return PublicReviewResponse.builder()
                    .reviewId(r.getId())
                    .reviewerName(reviewerName(b))
                    .reviewerAvatar(u != null ? u.getAvatarUrl() : null)
                    .rating(r.getRating())
                    .comment(r.getComment())
                    .adminResponse(r.getAdminResponse())
                    .departureDate(b.getSchedule().getDepartureDate())
                    .createdAt(r.getCreatedAt())
                    .build();
        });
    }

    // =====================================================================
    // KHÁCH VÃNG LAI (và cả khách có tài khoản) — đánh giá qua link trong email, xác thực bằng token của đơn
    // =====================================================================
    @Override
    public GuestReviewContextResponse getReviewContextByToken(String bookingCode, String token) {
        Booking b = bookingByToken(bookingCode, token);
        Schedule s = b.getSchedule();
        return GuestReviewContextResponse.builder()
                .bookingCode(b.getBookingCode())
                .bookingStatus(b.getBookingStatus())
                .reviewerName(reviewerName(b))
                .tourId(s.getTour().getTourId())
                .tourName(s.getTour().getTourName())
                .thumbnailUrl(s.getTour().getThumbnailUrl())
                .scheduleCode(s.getScheduleCode())
                .departureDate(s.getDepartureDate())
                .returnDate(s.getReturnDate())
                .review(ReviewResponse.of(reviewRepository.findByBooking_BookingId(b.getBookingId()).orElse(null)))
                .build();
    }

    /** Chưa có → tạo; có rồi → sửa (đang ẩn thì vẫn ẩn, admin xem lại) */
    @Override
    @Transactional
    public ReviewResponse saveReviewByToken(String bookingCode, String token, ReviewRequest request) {
        Booking b = bookingByToken(bookingCode, token);
        if (b.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        Review review = reviewRepository.findByBooking_BookingId(b.getBookingId()).orElse(null);
        try {
            if (review == null) {
                review = reviewRepository.saveAndFlush(Review.builder()
                        .booking(b)
                        .rating(request.getRating())
                        .comment(trimToNull(request.getComment()))
                        .build());
            } else {
                review.setRating(request.getRating());
                review.setComment(trimToNull(request.getComment()));
                reviewRepository.flush();
            }
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);   // bấm gửi 2 lần cùng lúc
        }
        syncTourRating(tourIdOf(b));
        return ReviewResponse.of(review);
    }

    /** Đúng mã đơn + đúng token (link trong email). Sai → "không tìm thấy", không lộ đơn có tồn tại */
    private Booking bookingByToken(String bookingCode, String token) {
        return bookingRepository.findWithDetailsByBookingCode(bookingCode.trim().toUpperCase())
                .filter(b -> tokenMatches(b.getPaymentToken(), token))
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    }

    private static boolean tokenMatches(String expected, String given) {
        return expected != null && given != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }

    /** Có tài khoản → họ tên tài khoản; khách vãng lai → Họ tên trong Thông tin liên lạc của đơn */
    private static String reviewerName(Booking b) {
        User u = b.getUser();
        if (u != null && StringUtils.hasText(u.getFullName())) return u.getFullName();
        return StringUtils.hasText(b.getContactName()) ? b.getContactName() : "Khách hàng";
    }

    @Override
    public TourReviewSummaryResponse getTourSummary(Integer tourId) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) counts.put(star, 0L);

        long total = 0, sum = 0;
        for (Object[] row : reviewRepository.countByRatingOfTour(tourId)) {
            int star = ((Number) row[0]).intValue();
            long n = ((Number) row[1]).longValue();
            counts.put(star, n);
            total += n;
            sum += star * n;
        }
        return TourReviewSummaryResponse.builder()
                .averageRating(total == 0 ? 0 : round1((double) sum / total))
                .totalReviews(total)
                .ratingCounts(counts)
                .build();
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================
    // =====================================================================
    // ĐỒNG BỘ tours.average_rating / tours.total_reviews
    // =====================================================================
    /**
     * Tính lại điểm TB + số đánh giá ĐANG HIỂN THỊ của tour rồi ghi vào bảng tours.
     * Gọi sau mỗi thao tác làm đổi kết quả: tạo, sửa số sao, ẩn, hiện.
     * Lưu sẵn trong tours (thay vì AVG mỗi lần đọc) vì trang danh sách / thẻ tour / sắp xếp theo
     * averageRating đều đọc cột này → không phải JOIN reviews cho từng tour.
     * Tính lại từ đầu (không cộng dồn) → luôn đúng, kể cả khi sửa số sao hay ẩn/hiện.
     */
    private void syncTourRating(Integer tourId) {
        reviewRepository.flush();   // thay đổi vừa rồi phải có trong DB trước khi AVG / COUNT
        Double avg  = reviewRepository.averageVisibleRating(tourId);
        long total  = reviewRepository.countForStats(tourId, false);
        BigDecimal rating = avg == null ? BigDecimal.ZERO : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        tourRepository.updateRating(tourId, rating, (int) total);
    }

    private static Integer tourIdOf(Booking b) {
        return b.getSchedule().getTour().getTourId();
    }

    private Review find(Integer reviewId) {
        return reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
    }

    /** sort = "createdAt,desc" | "rating,asc"; sai/thiếu → mới nhất trước */
    private Sort buildSort(String sort) {
        Sort fallback = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        if (!StringUtils.hasText(sort)) return fallback;
        String[] parts = sort.split(",");
        String field = SORTS.get(parts[0].trim());
        if (field == null) return fallback;
        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private ReviewAdminRowResponse toRow(Review r) {
        Booking b = r.getBooking();
        return ReviewAdminRowResponse.builder()
                .reviewId(r.getId())
                .createdAt(r.getCreatedAt())
                .rating(r.getRating())
                .comment(r.getComment())
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                .customerName(b.getContactName())
                .tourId(b.getSchedule().getTour().getTourId())
                .tourName(b.getSchedule().getTour().getTourName())
                .hidden(Boolean.TRUE.equals(r.getHidden()))
                .replied(StringUtils.hasText(r.getAdminResponse()))
                .build();
    }

    private ReviewAdminDetailResponse toDetail(Review r) {
        Booking b = r.getBooking();
        Schedule s = b.getSchedule();
        return ReviewAdminDetailResponse.builder()
                .reviewId(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .adminResponse(r.getAdminResponse())
                .hidden(Boolean.TRUE.equals(r.getHidden()))
                .hiddenReason(r.getHiddenReason())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .bookingId(b.getBookingId())
                .bookingCode(b.getBookingCode())
                .contactName(b.getContactName())
                .contactPhone(b.getContactPhone())
                .contactEmail(b.getContactEmail())
                .tourId(s.getTour().getTourId())
                .tourName(s.getTour().getTourName())
                .scheduleCode(s.getScheduleCode())
                .departureDate(s.getDepartureDate())
                .returnDate(s.getReturnDate())
                .build();
    }


    private static double round1(Double v) {
        return v == null ? 0 : Math.round(v * 10) / 10.0;
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
