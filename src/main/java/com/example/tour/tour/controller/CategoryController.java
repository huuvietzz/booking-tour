package com.example.tour.tour.controller;

import com.example.tour.common.dto.ApiResponse;
import com.example.tour.tour.dto.request.CategoryRequest;
import com.example.tour.tour.dto.response.CategoryResponse;
import com.example.tour.tour.service.CategoryService;
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
public class CategoryController {
    private final CategoryService categoryService;

    // API admin
    // Tạo danh mục (thông tin + ảnh trong 1 form)
    @PostMapping(value = "/admin/categories", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestPart("data") @Valid CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        CategoryResponse data = categoryService.createCategory(request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo danh mục thành công", data));
    }

    // Sửa danh mục
    @PutMapping(value = "/admin/categories/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Integer categoryId,
            @RequestPart("data") @Valid CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage) {
        CategoryResponse data = categoryService.updateCategory(categoryId, request, image, removeImage);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", data));
    }

    // Ẩn / hiện danh mục: PATCH /api/admin/categories/5/status?active=false
    @PatchMapping("/admin/categories/{categoryId}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> changeStatus(
            @PathVariable Integer categoryId,
            @RequestParam boolean active) {
        CategoryResponse data = categoryService.changeStatus(categoryId, active);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Đã hiển thị danh mục" : "Đã ẩn danh mục", data));
    }

    // Lấy danh sách danh mục(isActive = true / false)
    @GetMapping("/admin/categories")
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getAllCategoriesForAdmin(
            @RequestParam(required = false)              String keyword,
            @RequestParam(required = false)              Boolean isActive,
            @RequestParam(defaultValue = "0")            int page,
            @RequestParam(defaultValue = "10")           int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc")          String sortDir) {

        size = Math.min(Math.max(size, 1), 100);
        if (!Set.of("displayOrder", "categoryName", "createdAt").contains(sortBy)) {
            sortBy = "displayOrder";
        }
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        Page<CategoryResponse> data = categoryService.getAllCategoriesForAdmin(
                keyword, isActive, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", data));
    }

    // Lấy chi tiết danh mục(isActive = true / false)
    @GetMapping("/admin/categories/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryByIdForAdmin(@PathVariable Integer categoryId) {
        CategoryResponse data = categoryService.getCategoryByIdForAdmin(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết danh mục thành công", data));
    }

    // API public (danh mục có isActive = true)
    // Danh sách danh mục đang hiển thị (menu / bộ lọc phía khách)
    @GetMapping("/public/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công",
                categoryService.getAllCategories()));
    }

    // Lấy chi tiết danh mục
    @GetMapping("/public/categories/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Integer categoryId) {
        CategoryResponse data = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết danh mục thành công", data));
    }

}
