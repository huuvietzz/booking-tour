package com.example.tour.tour.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.tour.dto.request.LocationRequest;
import com.example.tour.tour.dto.response.LocationResponse;
import com.example.tour.tour.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class LocationController {

    private final LocationService locationService;

    // ======================= API ADMIN =======================

    // Tạo địa điểm (thông tin + ảnh trong 1 form)
    @PostMapping(value = "/admin/locations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(
            @RequestPart("data") @Valid LocationRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        LocationResponse data = locationService.createLocation(request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo địa điểm mới thành công", data));
    }

    // Sửa địa điểm
    @PutMapping(value = "/admin/locations/{locationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @PathVariable Integer locationId,
            @RequestPart("data") @Valid LocationRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage) {
        LocationResponse data = locationService.updateLocation(locationId, request, image, removeImage);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa điểm thành công", data));
    }

    // Ẩn / hiện địa điểm: PATCH /api/admin/locations/5/status?active=false
    @PatchMapping("/admin/locations/{locationId}/status")
    public ResponseEntity<ApiResponse<LocationResponse>> changeStatus(
            @PathVariable Integer locationId,
            @RequestParam boolean active) {
        LocationResponse data = locationService.changeStatus(locationId, active);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Đã hiển thị địa điểm" : "Đã ẩn địa điểm", data));
    }

    // Danh sách địa điểm (cả đang ẩn), có tìm kiếm + lọc trạng thái + phân trang
    @GetMapping("/admin/locations")
    public ResponseEntity<ApiResponse<Page<LocationResponse>>> getAllLocationsForAdmin(
            @RequestParam(required = false)              String keyword,
            @RequestParam(required = false)              Boolean isActive,
            @RequestParam(defaultValue = "0")            int page,
            @RequestParam(defaultValue = "10")           int size,
            @RequestParam(defaultValue = "locationName") String sortBy,
            @RequestParam(defaultValue = "asc")          String sortDir) {

        size = Math.min(Math.max(size, 1), 100);
        if (!Set.of("locationName", "createdAt").contains(sortBy)) {
            sortBy = "locationName";
        }
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Page<LocationResponse> data = locationService.getAllLocationsForAdmin(
                keyword, isActive, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa điểm thành công", data));
    }

    // Chi tiết địa điểm (cả đang ẩn)
    @GetMapping("/admin/locations/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationByIdForAdmin(@PathVariable Integer locationId) {
        LocationResponse data = locationService.getLocationByIdForAdmin(locationId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết địa điểm thành công", data));
    }

    // ======================= API PUBLIC (isActive = true) =======================

    // Danh sách địa điểm đang hiển thị (dropdown / bộ lọc phía khách)
    @GetMapping("/public/locations")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getAllLocations() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách địa điểm thành công",
                locationService.getAllLocations()));
    }

    // Chi tiết địa điểm
    @GetMapping("/public/locations/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationById(@PathVariable Integer locationId) {
        LocationResponse data = locationService.getLocationById(locationId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết địa điểm thành công", data));
    }
}