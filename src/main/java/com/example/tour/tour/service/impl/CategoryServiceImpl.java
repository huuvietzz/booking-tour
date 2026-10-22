package com.example.tour.tour.service.impl;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.common.util.SlugUtils;
import com.example.tour.tour.dto.request.CategoryRequest;
import com.example.tour.tour.dto.response.CategoryResponse;
import com.example.tour.tour.entity.Category;
import com.example.tour.tour.repository.CategoryRepository;
import com.example.tour.tour.service.CategoryService;
import com.example.tour.tour.specification.CategorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private static final String IMAGE_FOLDER = "categories";

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile image) {
        // Thứ tự không được trùng với danh mục đang hoạt động
        if (categoryRepository.existsByDisplayOrderAndIsActiveTrue(request.getDisplayOrder())) {
            throw new AppException(ErrorCode.DISPLAY_ORDER_ALREADY_EXISTS);
        }

        String slug = SlugUtils.toSlug(request.getSlug());
        if (slug.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_SLUG);
        }
        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.SLUG_ALREADY_EXISTS);
        }
        request.setSlug(slug);

        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());

        // Có chọn ảnh mới lưu file. Kiểm tra dữ liệu XONG mới lưu → dữ liệu sai thì không sinh file thừa
        String newUrl = null;
        if (image != null && !image.isEmpty()) {
            newUrl = fileStorageService.storeImage(image, IMAGE_FOLDER);
            category.setImageUrl(newUrl);
        }

        Category savedCategory = categoryRepository.save(category);

        // Lỗi DB khi commit → xoá file vừa lưu; thành công → giữ nguyên
        fileStorageService.cleanupAfterTransaction(null, newUrl);
        return toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    public Page<CategoryResponse> getAllCategoriesForAdmin(String keyword, Boolean isActive, Pageable pageable) {
        Specification<Category> spec = CategorySpecification.filter(keyword, isActive);
        return categoryRepository.findAll(spec, pageable)
                .map(this::toCategoryResponse);
    }

    @Override
    public CategoryResponse getCategoryById(int categoryId) {
        // check category có isActive = true
        Category category = categoryRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toCategoryResponse(category);
    }

    @Override
    public CategoryResponse getCategoryByIdForAdmin(int categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(int categoryId, CategoryRequest request,
                                           MultipartFile image, boolean removeImage) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (categoryRepository.existsByDisplayOrderAndIsActiveTrueAndCategoryIdNot(
                request.getDisplayOrder(), categoryId)) {
            throw new AppException(ErrorCode.DISPLAY_ORDER_ALREADY_EXISTS);
        }

        String slug = SlugUtils.toSlug(request.getSlug());
        if (slug.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_SLUG);
        }
        if (categoryRepository.existsBySlugAndCategoryIdNot(slug, categoryId)) {
            throw new AppException(ErrorCode.SLUG_ALREADY_EXISTS);
        }

        category.setCategoryName(request.getCategoryName());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setIsActive(request.getIsActive());

        // ----- Ảnh -----
        String oldUrl  = category.getImageUrl();
        String newUrl  = null;
        String toDelete = null;   // ảnh cũ cần xoá SAU KHI commit thành công

        if (image != null && !image.isEmpty()) {
            newUrl = fileStorageService.storeImage(image, IMAGE_FOLDER);   // chọn ảnh mới → thay ảnh cũ
            category.setImageUrl(newUrl);
            toDelete = oldUrl;
        } else if (removeImage) {
            category.setImageUrl(null);                                     // bấm xoá ảnh
            toDelete = oldUrl;
        }
        // Không chọn ảnh, không xoá → giữ nguyên ảnh cũ

        Category savedCategory = categoryRepository.save(category);

        fileStorageService.cleanupAfterTransaction(toDelete, newUrl);
        return toCategoryResponse(savedCategory);
    }

    /**
     * Ẩn / hiện danh mục (thay cho xoá).
     * Ẩn: dừng hiển thị và dừng nhận đơn mới; tour, lịch khởi hành, đơn đã đặt giữ nguyên.
     */
    @Override
    @Transactional
    public CategoryResponse changeStatus(int categoryId, boolean active) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Hiện lại một danh mục đang ẩn: thứ tự của nó có thể đã bị danh mục khác dùng mất
        if (active && !Boolean.TRUE.equals(category.getIsActive())
                && categoryRepository.existsByDisplayOrderAndIsActiveTrueAndCategoryIdNot(
                category.getDisplayOrder(), categoryId)) {
            throw new AppException(ErrorCode.DISPLAY_ORDER_ALREADY_EXISTS);
        }

        category.setIsActive(active);
        return toCategoryResponse(categoryRepository.save(category));
    }

    // Entity -> Response
    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}