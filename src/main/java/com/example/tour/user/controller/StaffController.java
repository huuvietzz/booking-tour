package com.example.tour.user.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.user.dto.request.StaffCreateRequest;
import com.example.tour.user.dto.request.StaffFilterRequest;
import com.example.tour.user.dto.request.StaffUpdateRequest;
import com.example.tour.user.dto.response.StaffResponse;
import com.example.tour.user.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Quản lý nhân viên: CHỈ ADMIN. STAFF gọi vào → 403.
 * @PreAuthorize ở cấp class áp dụng cho mọi method → không sót endpoint nào.
 * Cần @EnableMethodSecurity trong SecurityConfig (xem file SECURITY-...txt).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {

    private static final Set<String> SORTS = Set.of("createdAt", "fullName", "email", "lastLogin");

    private final StaffService staffService;

    // ?keyword=&role=STAFF&isActive=true&gender=&page=0&size=10&sortBy=createdAt&sortDir=desc
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StaffResponse>>> getStaffs(
            @ModelAttribute StaffFilterRequest filter,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhân viên thành công",
                staffService.getStaffs(filter, buildPageable(page, size, sortBy, sortDir))));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaff(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin nhân viên thành công", staffService.getStaff(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(@RequestBody @Valid StaffCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm nhân viên thành công", staffService.createStaff(request)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(@PathVariable Integer userId,
                                                                  @RequestBody @Valid StaffUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật nhân viên thành công", staffService.updateStaff(userId, request)));
    }

    /** Khoá / mở khoá: PATCH /api/admin/staff/5/active?value=false */
    @PatchMapping("/{userId}/active")
    public ResponseEntity<ApiResponse<StaffResponse>> setActive(@PathVariable Integer userId, @RequestParam boolean value) {
        return ResponseEntity.ok(ApiResponse.success(value ? "Đã mở khoá tài khoản" : "Đã khoá tài khoản",
                staffService.setActive(userId, value)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable Integer userId) {
        staffService.deleteStaff(userId);
        return ResponseEntity.ok(ApiResponse.success("Xoá nhân viên thành công", null));
    }

    private static Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        if (!SORTS.contains(sortBy)) sortBy = "createdAt";
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        sort = sort.and(Sort.by("userId").descending());
        return PageRequest.of(page, size, sort);
    }
}
