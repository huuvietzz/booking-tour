package com.example.tour.promotion.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.promotion.dto.request.PromotionCheckRequest;
import com.example.tour.promotion.dto.request.PromotionFilterRequest;
import com.example.tour.promotion.dto.request.PromotionRequest;
import com.example.tour.promotion.dto.response.PromotionCheckResponse;
import com.example.tour.promotion.dto.response.PromotionPublicResponse;
import com.example.tour.promotion.dto.response.PromotionResponse;
import com.example.tour.promotion.service.PromotionService;
import com.example.tour.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PromotionController {

    private static final Set<String> ADMIN_SORTS =
            Set.of("createdAt", "startDate", "endDate", "promotionCode", "discountValue", "usedCount");

    private final PromotionService promotionService;

    // ======================= ADMIN =======================

    @PostMapping("/admin/promotions")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(@RequestBody @Valid PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo khuyến mãi thành công", promotionService.createPromotion(request)));
    }

    @PutMapping("/admin/promotions/{promotionId}")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(@PathVariable Integer promotionId,
                                                                          @RequestBody @Valid PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khuyến mãi thành công",
                promotionService.updatePromotion(promotionId, request)));
    }

    /** Bật / tắt nhanh từ bảng danh sách: PATCH /api/admin/promotions/5/active?value=false */
    @PatchMapping("/admin/promotions/{promotionId}/active")
    public ResponseEntity<ApiResponse<PromotionResponse>> setActive(@PathVariable Integer promotionId,
                                                                    @RequestParam boolean value) {
        return ResponseEntity.ok(ApiResponse.success(value ? "Đã bật khuyến mãi" : "Đã tắt khuyến mãi",
                promotionService.setActive(promotionId, value)));
    }

    @GetMapping("/admin/promotions/{promotionId}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionForAdmin(@PathVariable Integer promotionId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy khuyến mãi thành công",
                promotionService.getPromotionForAdmin(promotionId)));
    }

    // ?keyword=&discountType=PERCENT&status=RUNNING&tourId=&page=0&size=10&sortBy=createdAt&sortDir=desc
    @GetMapping("/admin/promotions")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> getPromotionsForAdmin(
            @ModelAttribute PromotionFilterRequest filter,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khuyến mãi thành công",
                promotionService.getPromotionsForAdmin(filter, buildPageable(page, size, sortBy, sortDir))));
    }

    @DeleteMapping("/admin/promotions/{promotionId}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Integer promotionId) {
        promotionService.deletePromotion(promotionId);
        return ResponseEntity.ok(ApiResponse.success("Xoá khuyến mãi thành công", null));
    }

    // ======================= KHÁCH =======================

    /** Mã đang dùng được (trang ưu đãi / gợi ý mã ở trang đặt tour). ?tourId=5 → chỉ mã áp dụng cho tour 5 */
    @GetMapping("/public/promotions")
    public ResponseEntity<ApiResponse<List<PromotionPublicResponse>>> getUsablePromotions(
            @RequestParam(required = false) Integer tourId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khuyến mãi thành công",
                promotionService.getUsablePromotions(tourId)));
    }

    /**
     * Nút "Áp dụng" ở trang đặt tour. Cần đăng nhập để kiểm tra "mỗi khách dùng tối đa N lần";
     * userId lấy từ token, không nhận từ body → không ai kiểm tra hộ / dùng hộ lượt của người khác.
     */
    @PostMapping("/user/promotions/check")
    public ResponseEntity<ApiResponse<PromotionCheckResponse>> checkPromotion(@AuthenticationPrincipal User user,
                                                                              @RequestBody @Valid PromotionCheckRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Áp dụng mã giảm giá thành công",
                promotionService.checkPromotion(request, user.getUserId())));
    }

    // ----- Hỗ trợ -----
    private static Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        if (!ADMIN_SORTS.contains(sortBy)) sortBy = "createdAt";   // chặn sort theo field lạ → lỗi 500
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        sort = sort.and(Sort.by("promotionId").descending());   // trùng giá trị → thứ tự vẫn cố định khi phân trang
        return PageRequest.of(page, size, sort);
    }
}