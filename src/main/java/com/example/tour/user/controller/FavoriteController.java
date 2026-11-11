package com.example.tour.user.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.tour.dto.response.TourCardResponse;
import com.example.tour.user.entity.User;
import com.example.tour.user.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tour yêu thích của khách đang đăng nhập. Mọi API ở đây BẮT BUỘC đăng nhập.
 * userId luôn lấy từ token, KHÔNG nhận từ request → không ai xem / sửa được danh sách của người khác.
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** Thêm vào yêu thích. PUT vì gọi nhiều lần kết quả vẫn như 1 lần (chỉ 1 bản ghi). */
    @PutMapping("/{tourId}")
    public ResponseEntity<ApiResponse<Void>> addFavorite(@AuthenticationPrincipal User user,
                                                         @PathVariable Integer tourId) {
        favoriteService.addFavorite(user.getUserId(), tourId);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào tour yêu thích", null));
    }

    /** Bỏ yêu thích. Chưa thích mà gọi vẫn trả thành công. */
    @DeleteMapping("/{tourId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@AuthenticationPrincipal User user,
                                                            @PathVariable Integer tourId) {
        favoriteService.removeFavorite(user.getUserId(), tourId);
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ khỏi tour yêu thích", null));
    }

    /** Trang "Tour yêu thích": ?page=0&size=12, mới thích xếp trước */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TourCardResponse>>> getMyFavorites(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<TourCardResponse> data = favoriteService.getMyFavorites(user.getUserId(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));   // không kèm Sort: query đã ORDER BY
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tour yêu thích thành công", data));
    }

    /** Id các tour đã thích → tô đỏ tim khi mở tours.html / index.html */
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Integer>>> getMyFavoriteTourIds(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tour yêu thích thành công",
                favoriteService.getMyFavoriteTourIds(user.getUserId())));
    }
}
