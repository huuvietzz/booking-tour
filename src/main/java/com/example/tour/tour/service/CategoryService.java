package com.example.tour.tour.service;

import com.example.tour.tour.dto.request.CategoryRequest;
import com.example.tour.tour.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request, MultipartFile image);

    // ĐỔI: phía khách lấy toàn bộ danh mục đang hiển thị, không phân trang
    List<CategoryResponse> getAllCategories();

    Page<CategoryResponse> getAllCategoriesForAdmin(String keyword, Boolean isActive, Pageable pageable);

    CategoryResponse getCategoryById(int categoryId);

    CategoryResponse getCategoryByIdForAdmin(int categoryId);

    CategoryResponse updateCategory(int categoryId, CategoryRequest request,
                                    MultipartFile image, boolean removeImage);

    // THÊM: ẩn / hiện danh mục
    CategoryResponse changeStatus(int categoryId, boolean active);
}
