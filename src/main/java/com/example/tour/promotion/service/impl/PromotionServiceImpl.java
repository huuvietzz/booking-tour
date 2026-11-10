package com.example.tour.promotion.service.impl;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.promotion.constant.DiscountType;
import com.example.tour.promotion.dto.request.PromotionCheckRequest;
import com.example.tour.promotion.dto.request.PromotionFilterRequest;
import com.example.tour.promotion.dto.request.PromotionRequest;
import com.example.tour.promotion.dto.response.PromotionCheckResponse;
import com.example.tour.promotion.dto.response.PromotionPublicResponse;
import com.example.tour.promotion.dto.response.PromotionResponse;
import com.example.tour.promotion.entity.Promotion;
import com.example.tour.promotion.repository.PromotionRepository;
import com.example.tour.promotion.service.PromotionService;
import com.example.tour.promotion.specification.PromotionSpecification;
import com.example.tour.tour.entity.Tour;
import com.example.tour.tour.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final TourRepository tourRepository;

    // =====================================================================
    // ADMIN
    // =====================================================================

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        String code = normalizeCode(request.getPromotionCode());
        if (promotionRepository.existsByPromotionCode(code)) throw new AppException(ErrorCode.PROMOTION_CODE_EXISTS);
        validateRules(request);

        Promotion promotion = new Promotion();
        promotion.setUsedCount(0);   // mã mới luôn bắt đầu từ 0 lượt, không nhận từ request
        applyRequest(promotion, request, code);
        return toResponse(promotionRepository.save(promotion));
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Integer promotionId, PromotionRequest request) {
        Promotion promotion = findOrThrow(promotionId);
        String code = normalizeCode(request.getPromotionCode());
        if (promotionRepository.existsByPromotionCodeAndPromotionIdNot(code, promotionId)) {
            throw new AppException(ErrorCode.PROMOTION_CODE_EXISTS);
        }
        validateRules(request);
        // Đã có khách dùng: hạ quantity xuống dưới số đã dùng thì usedCount > quantity → dữ liệu vô lý
        if (request.getQuantity() != null && request.getQuantity() < promotion.getUsedCount()) {
            throw new AppException(ErrorCode.PROMOTION_QUANTITY_TOO_LOW);
        }
        // Đã có khách dùng thì không cho đổi mã: đơn cũ, email đã gửi khách vẫn ghi mã cũ
        if (promotion.getUsedCount() > 0 && !promotion.getPromotionCode().equals(code)) {
            throw new AppException(ErrorCode.PROMOTION_CODE_LOCKED);
        }
        applyRequest(promotion, request, code);
        return toResponse(promotion);   // entity đang được quản lý → commit tự UPDATE, không cần save()
    }

    @Override
    @Transactional
    public PromotionResponse setActive(Integer promotionId, boolean active) {
        Promotion promotion = findOrThrow(promotionId);
        promotion.setIsActive(active);
        return toResponse(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponse getPromotionForAdmin(Integer promotionId) {
        return toResponse(findOrThrow(promotionId));
    }

    @Override
    @Transactional(readOnly = true)   // toResponse đọc p.getTours() (LAZY) → cần session còn mở
    public Page<PromotionResponse> getPromotionsForAdmin(PromotionFilterRequest filter, Pageable pageable) {
        return promotionRepository.findAll(PromotionSpecification.adminFilter(filter), pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public void deletePromotion(Integer promotionId) {
        Promotion promotion = findOrThrow(promotionId);
        // Đơn cũ đang trỏ tới mã này (khoá ngoại) → xoá sẽ lỗi / mất lịch sử. Muốn ngừng thì TẮT mã.
        if (promotion.getUsedCount() > 0) throw new AppException(ErrorCode.PROMOTION_IN_USE);
        promotionRepository.delete(promotion);   // dòng ở bảng promotion_tours tự xoá theo (Promotion là owning side)
    }

    // =====================================================================
    // KHÁCH
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<PromotionPublicResponse> getUsablePromotions(Integer tourId) {
        return promotionRepository.findAll(PromotionSpecification.usable(tourId), Sort.by("endDate").ascending())
                .stream().map(this::toPublicResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionCheckResponse checkPromotion(PromotionCheckRequest request, Integer userId) {
        Promotion p = validateForBooking(request.getPromotionCode(), request.getTourId(), request.getOrderAmount(), userId);
        BigDecimal discount = calculateDiscount(p, request.getOrderAmount());
        return PromotionCheckResponse.builder()
                .promotionCode(p.getPromotionCode())
                .promotionName(p.getPromotionName())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .orderAmount(request.getOrderAmount())
                .discountAmount(discount)
                .finalAmount(request.getOrderAmount().subtract(discount))
                .build();
    }

    // =====================================================================
    // DÙNG CHO BOOKING
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Promotion validateForBooking(String promotionCode, Integer tourId, BigDecimal orderAmount, Integer userId) {
        // Không có / đang tắt → cùng 1 thông báo "không hợp lệ": không cho người ngoài dò mã nào đang tồn tại
        Promotion p = promotionRepository.findByPromotionCode(normalizeCode(promotionCode))
                .filter(promo -> Boolean.TRUE.equals(promo.getIsActive()))
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_INVALID));

        LocalDateTime now = LocalDateTime.now();
        if (p.getStartDate().isAfter(now)) throw new AppException(ErrorCode.PROMOTION_NOT_STARTED);
        if (p.getEndDate().isBefore(now)) throw new AppException(ErrorCode.PROMOTION_EXPIRED);
        if (p.getQuantity() != null && p.getUsedCount() >= p.getQuantity()) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);
        }
        if (!p.getTours().isEmpty() && p.getTours().stream().noneMatch(t -> t.getTourId().equals(tourId))) {
            throw new AppException(ErrorCode.PROMOTION_NOT_APPLICABLE_TOUR);
        }
        if (p.getMinOrderAmount() != null && orderAmount.compareTo(p.getMinOrderAmount()) < 0) {
            throw new AppException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }
        if (userId != null && p.getMaxUsesPerUser() != null
                && promotionRepository.countActiveUsesByUser(p.getPromotionId(), userId) >= p.getMaxUsesPerUser()) {
            throw new AppException(ErrorCode.PROMOTION_USER_LIMIT_REACHED);
        }
        return p;
    }

    @Override
    public BigDecimal calculateDiscount(Promotion p, BigDecimal orderAmount) {
        BigDecimal discount;
        if (p.getDiscountType() == DiscountType.PERCENT) {
            // Làm tròn XUỐNG đến đồng: 1.234.567 x 10% = 123.456,7 → giảm 123.456
            discount = orderAmount.multiply(p.getDiscountValue()).divide(HUNDRED, 0, RoundingMode.DOWN);
            if (p.getMaxDiscountAmount() != null) discount = discount.min(p.getMaxDiscountAmount());
        } else {
            discount = p.getDiscountValue();
        }
        return discount.min(orderAmount);   // FIXED 500k cho đơn 300k → giảm 300k, không để tổng tiền âm
    }

    @Override
    @Transactional
    public void consume(Integer promotionId) {
        if (promotionRepository.increaseUsedCount(promotionId) == 0) {
            throw new AppException(ErrorCode.PROMOTION_OUT_OF_STOCK);   // người khác vừa lấy lượt cuối
        }
    }

    @Override
    @Transactional
    public void release(Integer promotionId) {
        promotionRepository.decreaseUsedCount(promotionId);
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================

    private Promotion findOrThrow(Integer promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    /** "  sale10 " → "SALE10": khách gõ chữ thường vẫn khớp, DB không có 2 mã chỉ khác hoa / thường */
    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    /** Các quy tắc liên quan nhiều field — annotation trên từng field không kiểm được */
    private static void validateRules(PromotionRequest r) {
        if (!r.getEndDate().isAfter(r.getStartDate())) throw new AppException(ErrorCode.INVALID_PROMOTION_DATE);
        if (r.getDiscountType() == DiscountType.PERCENT && r.getDiscountValue().compareTo(HUNDRED) > 0) {
            throw new AppException(ErrorCode.INVALID_DISCOUNT_PERCENT);
        }
    }

    private void applyRequest(Promotion p, PromotionRequest r, String code) {
        p.setPromotionCode(code);
        p.setPromotionName(r.getPromotionName().trim());
        // Mô tả không bắt buộc: bỏ trống → lưu NULL (không lưu chuỗi rỗng)
        p.setDescription(r.getDescription() == null || r.getDescription().isBlank() ? null : r.getDescription().trim());
        p.setDiscountType(r.getDiscountType());
        p.setDiscountValue(r.getDiscountValue());
        // Giảm tối đa chỉ có nghĩa với PERCENT; FIXED đã là số tiền cố định
        p.setMaxDiscountAmount(r.getDiscountType() == DiscountType.PERCENT ? r.getMaxDiscountAmount() : null);
        p.setMinOrderAmount(r.getMinOrderAmount() != null ? r.getMinOrderAmount() : BigDecimal.ZERO);
        p.setStartDate(r.getStartDate());
        p.setEndDate(r.getEndDate());
        p.setQuantity(r.getQuantity());
        p.setMaxUsesPerUser(r.getMaxUsesPerUser());
        p.setIsActive(r.getIsActive() == null || r.getIsActive());
        p.setTours(loadTours(r.getTourIds()));
    }

    /** id → entity; có id không tồn tại thì báo lỗi luôn, không âm thầm bỏ qua */
    private Set<Tour> loadTours(Set<Integer> tourIds) {
        if (tourIds == null || tourIds.isEmpty()) return new HashSet<>();
        List<Tour> tours = tourRepository.findAllById(tourIds);
        if (tours.size() != tourIds.size()) throw new AppException(ErrorCode.TOUR_NOT_FOUND);
        return new HashSet<>(tours);
    }

    private PromotionResponse toResponse(Promotion p) {
        List<PromotionResponse.TourItem> tours = p.getTours().stream()
                .map(t -> new PromotionResponse.TourItem(t.getTourId(), t.getTourCode(), t.getTourName()))
                .sorted(Comparator.comparing(PromotionResponse.TourItem::tourId))
                .toList();
        return PromotionResponse.builder()
                .promotionId(p.getPromotionId())
                .promotionCode(p.getPromotionCode())
                .promotionName(p.getPromotionName())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderAmount(p.getMinOrderAmount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .quantity(p.getQuantity())
                .maxUsesPerUser(p.getMaxUsesPerUser())
                .usedCount(p.getUsedCount())
                .isActive(p.getIsActive())
                .appliesToAllTours(tours.isEmpty())
                .tours(tours)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PromotionPublicResponse toPublicResponse(Promotion p) {
        return PromotionPublicResponse.builder()
                .promotionCode(p.getPromotionCode())
                .promotionName(p.getPromotionName())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderAmount(p.getMinOrderAmount())
                .endDate(p.getEndDate())
                .build();
    }
}
