package com.example.tour.tour.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.tour.dto.request.TourFilterRequest;
import com.example.tour.tour.dto.request.TourRequest;
import com.example.tour.tour.dto.response.TourCardResponse;
import com.example.tour.tour.dto.response.TourResponse;
import com.example.tour.tour.dto.response.TourAdminRowResponse;
import com.example.tour.tour.service.TourService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TourController {

    private static final Set<String> ADMIN_SORTS  = Set.of("createdAt", "tourName", "tourCode", "basePrice", "viewCount");
    private static final Set<String> PUBLIC_SORTS = Set.of("createdAt", "basePrice", "viewCount", "averageRating");

    private final TourService tourService;

    // ======================= API ADMIN =======================

    /**
     * Thêm tour — multipart:
     *   data          : JSON TourRequest (bắt buộc)
     *   thumbnail     : 1 file ảnh đại diện (không bắt buộc)
     *   galleryImages : nhiều file ảnh thư viện (không bắt buộc)
     */
    @PostMapping(value = "/admin/tours", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourResponse>> createTour(
            @RequestPart("data") @Valid TourRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "galleryImages", required = false) List<MultipartFile> galleryImages) {
        TourResponse data = tourService.createTour(request, thumbnail, galleryImages);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo tour thành công", data));
    }

    /**
     * Sửa tour — multipart:
     *   data            : JSON TourRequest, có keepImageIds = ảnh thư viện cũ giữ lại (đúng thứ tự)
     *   thumbnail       : ảnh đại diện mới (không gửi = giữ ảnh cũ)
     *   removeThumbnail : "true" khi bỏ ảnh đại diện mà không chọn ảnh mới
     *   galleryImages   : ảnh thư viện mới, thêm vào sau các ảnh giữ lại
     */
    @PutMapping(value = "/admin/tours/{tourId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TourResponse>> updateTour(
            @PathVariable Integer tourId,
            @RequestPart("data") @Valid TourRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "removeThumbnail", defaultValue = "false") boolean removeThumbnail,
            @RequestPart(value = "galleryImages", required = false) List<MultipartFile> galleryImages) {
        TourResponse data = tourService.updateTour(tourId, request, thumbnail, removeThumbnail, galleryImages);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tour thành công", data));
    }

    // Xoá cứng: chỉ khi tour chưa có booking
    @DeleteMapping("/admin/tours/{tourId}")
    public ResponseEntity<ApiResponse<Void>> deleteTour(@PathVariable Integer tourId) {
        tourService.deleteTour(tourId);
        return ResponseEntity.ok(ApiResponse.success("Xoá tour thành công", null));
    }

    // Ẩn / hiện: PATCH /api/admin/tours/5/status?active=false
    @PatchMapping("/admin/tours/{tourId}/status")
    public ResponseEntity<ApiResponse<TourResponse>> changeStatus(
            @PathVariable Integer tourId,
            @RequestParam boolean active) {
        TourResponse data = tourService.changeStatus(tourId, active);
        return ResponseEntity.ok(ApiResponse.success(active ? "Đã hiển thị tour" : "Đã ẩn tour", data));
    }

    // Danh sách: ?keyword=&categoryId=&locationId=&tourClass=&isFeatured=&isActive=&page=&size=&sortBy=&sortDir=
    @GetMapping("/admin/tours")
    public ResponseEntity<ApiResponse<Page<TourAdminRowResponse>>> getAllToursForAdmin(
            @ModelAttribute TourFilterRequest filter,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, ADMIN_SORTS);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tour thành công",
                tourService.getAllToursForAdmin(filter, pageable)));
    }

    @GetMapping("/admin/tours/{tourId}")
    public ResponseEntity<ApiResponse<TourResponse>> getTourByIdForAdmin(@PathVariable Integer tourId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết tour thành công",
                tourService.getTourByIdForAdmin(tourId)));
    }

    // ======================= API PUBLIC =======================

    // ?keyword=&categoryId=&locationId=&tourClass=&isFeatured=&minPrice=&maxPrice=&page=&size=&sortBy=&sortDir=
    @GetMapping("/public/tours")
    public ResponseEntity<ApiResponse<Page<TourCardResponse>>> getAllTours(
            @ModelAttribute TourFilterRequest filter,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "12")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        filter.setIsActive(null);   // phía khách không được chọn xem tour đang ẩn
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, PUBLIC_SORTS);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tour thành công",
                tourService.getAllTours(filter, pageable)));
    }

    @GetMapping("/public/tours/{tourId}")
    public ResponseEntity<ApiResponse<TourResponse>> getTourById(@PathVariable Integer tourId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết tour thành công",
                tourService.getTourById(tourId)));
    }

    // ----- Hỗ trợ -----
    private static Pageable buildPageable(int page, int size, String sortBy, String sortDir, Set<String> allowed) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        if (!allowed.contains(sortBy)) sortBy = "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        sort = sort.and(Sort.by("tourId").descending());   // trùng giá trị → tour có id lớn hơn (tạo sau) xếp trước
        return PageRequest.of(page, size, sort);
    }
}