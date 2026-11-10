package com.example.tour.promotion.service;

import com.example.tour.promotion.dto.request.PromotionCheckRequest;
import com.example.tour.promotion.dto.request.PromotionFilterRequest;
import com.example.tour.promotion.dto.request.PromotionRequest;
import com.example.tour.promotion.dto.response.PromotionCheckResponse;
import com.example.tour.promotion.dto.response.PromotionPublicResponse;
import com.example.tour.promotion.dto.response.PromotionResponse;
import com.example.tour.promotion.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {

    // ===== ADMIN =====
    PromotionResponse createPromotion(PromotionRequest request);

    PromotionResponse updatePromotion(Integer promotionId, PromotionRequest request);

    PromotionResponse setActive(Integer promotionId, boolean active);

    PromotionResponse getPromotionForAdmin(Integer promotionId);

    Page<PromotionResponse> getPromotionsForAdmin(PromotionFilterRequest filter, Pageable pageable);

    void deletePromotion(Integer promotionId);

    // ===== KHÁCH =====
    /** Mã đang dùng được; tourId != null → chỉ mã áp dụng cho tour đó */
    List<PromotionPublicResponse> getUsablePromotions(Integer tourId);

    /** Nút "Áp dụng" ở trang đặt tour: kiểm tra mã và tính thử tiền giảm */
    PromotionCheckResponse checkPromotion(PromotionCheckRequest request, Integer userId);

    // ===== DÙNG CHO BookingService (lúc tạo / huỷ đơn) =====
    /** Kiểm tra đủ điều kiện, sai thì ném AppException. Trả entity để gắn vào Booking */
    Promotion validateForBooking(String promotionCode, Integer tourId, BigDecimal orderAmount, Integer userId);

    BigDecimal calculateDiscount(Promotion promotion, BigDecimal orderAmount);

    /** Lấy 1 lượt (gọi TRONG transaction tạo đơn, sau validateForBooking) */
    void consume(Integer promotionId);

    /** Trả lại 1 lượt khi đơn bị huỷ */
    void release(Integer promotionId);
}
