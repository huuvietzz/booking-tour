package com.example.tour.user.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.user.dto.request.CustomerCreateRequest;
import com.example.tour.user.dto.request.CustomerFilterRequest;
import com.example.tour.user.dto.request.CustomerUpdateRequest;
import com.example.tour.user.dto.response.CustomerResponse;
import com.example.tour.user.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Quản lý khách hàng: ADMIN và STAFF đều dùng được.
 * /api/admin/** đã được SecurityConfig giới hạn cho ADMIN + STAFF → không cần @PreAuthorize ở đây.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/customers")
public class CustomerController {

    private static final Set<String> SORTS = Set.of("createdAt", "fullName", "email", "lastLogin");

    private final CustomerService customerService;

    // ?keyword=&isActive=true&emailVerified=&gender=MALE&page=0&size=10&sortBy=createdAt&sortDir=desc
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomers(
            @ModelAttribute CustomerFilterRequest filter,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khách hàng thành công",
                customerService.getCustomers(filter, buildPageable(page, size, sortBy, sortDir))));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin khách hàng thành công",
                customerService.getCustomer(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(@RequestBody @Valid CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Thêm khách hàng thành công", customerService.createCustomer(request)));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(@PathVariable Integer userId,
                                                                        @RequestBody @Valid CustomerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khách hàng thành công",
                customerService.updateCustomer(userId, request)));
    }

    /** Khoá / mở khoá nhanh từ công tắc trong bảng: PATCH /api/admin/customers/5/active?value=false */
    @PatchMapping("/{userId}/active")
    public ResponseEntity<ApiResponse<CustomerResponse>> setActive(@PathVariable Integer userId,
                                                                   @RequestParam boolean value) {
        return ResponseEntity.ok(ApiResponse.success(value ? "Đã mở khoá tài khoản" : "Đã khoá tài khoản",
                customerService.setActive(userId, value)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Integer userId) {
        customerService.deleteCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success("Xoá khách hàng thành công", null));
    }

    // ----- Hỗ trợ -----
    private static Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
        if (!SORTS.contains(sortBy)) sortBy = "createdAt";   // chặn sort theo field lạ → lỗi 500
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        sort = sort.and(Sort.by("userId").descending());     // trùng giá trị → thứ tự vẫn cố định khi phân trang
        return PageRequest.of(page, size, sort);
    }
}
