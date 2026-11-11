package com.example.tour.user.service.impl;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.common.util.SecurityUtils;
import com.example.tour.user.constant.Role;
import com.example.tour.user.dto.request.StaffCreateRequest;
import com.example.tour.user.dto.request.StaffFilterRequest;
import com.example.tour.user.dto.request.StaffUpdateRequest;
import com.example.tour.user.dto.response.StaffResponse;
import com.example.tour.user.entity.User;
import com.example.tour.user.repository.UserRepository;
import com.example.tour.user.repository.UserTokenRepository;
import com.example.tour.user.service.StaffService;
import com.example.tour.user.specification.StaffSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private static final List<Role> STAFF_ROLES = List.of(Role.ADMIN, Role.STAFF);

    private final UserRepository userRepository;
    private final UserTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    // =====================================================================
    // XEM
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<StaffResponse> getStaffs(StaffFilterRequest filter, Pageable pageable) {
        Integer me = SecurityUtils.getCurrentUserId();
        return userRepository.findAll(StaffSpecification.adminFilter(filter), pageable).map(u -> toResponse(u, me));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaff(Integer userId) {
        return toResponse(findStaff(userId), SecurityUtils.getCurrentUserId());
    }

    // =====================================================================
    // THÊM / SỬA
    // =====================================================================

    @Override
    @Transactional
    public StaffResponse createStaff(StaffCreateRequest request) {
        checkStaffRole(request.getRole());
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setAddress(blankToNull(request.getAddress()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setActive(request.getIsActive() == null || request.getIsActive());
        // Tài khoản nội bộ do admin tạo: không cần xác thực email.
        // Bắt buộc true: User.isEnabled() = emailVerified → false thì nhân viên không đăng nhập được
        user.setEmailVerified(true);

        return toResponse(userRepository.save(user), SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(Integer userId, StaffUpdateRequest request) {
        checkStaffRole(request.getRole());
        Integer me = SecurityUtils.getCurrentUserId();
        User user = findStaff(userId);
        boolean self = Objects.equals(user.getUserId(), me);
        boolean newActive = request.getIsActive() == null || request.getIsActive();

        if (self) {
            // Tự hạ mình xuống STAFF / tự khoá → mất quyền vào trang này ngay lập tức
            if (request.getRole() != user.getRole() || !newActive) throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
            // Đổi email của chính mình ở đây thì token đang dùng (subject = email cũ) hỏng ngay
            // → đổi ở trang Hồ sơ (AdminService.updateMyProfile cấp token mới)
            if (!normalizeEmail(request.getEmail()).equalsIgnoreCase(user.getEmail())) {
                throw new AppException(ErrorCode.SELF_EMAIL_CHANGE_IN_PROFILE);
            }
        }
        // ADMIN bị hạ xuống STAFF hoặc bị khoá → phải còn ADMIN khác đang hoạt động
        if (user.getRole() == Role.ADMIN && user.isActive() && (request.getRole() != Role.ADMIN || !newActive)) {
            ensureAnotherActiveAdmin();
        }

        String email = normalizeEmail(request.getEmail());
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (!request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setAddress(blankToNull(request.getAddress()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setActive(newActive);
        // password, emailVerified KHÔNG sửa ở đây

        return toResponse(user, me);   // entity đang được quản lý → commit tự UPDATE
    }

    @Override
    @Transactional
    public StaffResponse setActive(Integer userId, boolean active) {
        Integer me = SecurityUtils.getCurrentUserId();
        User user = findStaff(userId);
        if (!active) {
            if (Objects.equals(user.getUserId(), me)) throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
            if (user.getRole() == Role.ADMIN && user.isActive()) ensureAnotherActiveAdmin();
        }
        user.setActive(active);
        return toResponse(user, me);
    }

    // =====================================================================
    // XOÁ
    // =====================================================================

    @Override
    @Transactional
    public void deleteStaff(Integer userId) {
        User user = findStaff(userId);
        if (Objects.equals(user.getUserId(), SecurityUtils.getCurrentUserId())) throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
        if (user.getRole() == Role.ADMIN && user.isActive()) ensureAnotherActiveAdmin();

        tokenRepository.deleteByUser_UserId(userId);
        String avatarUrl = user.getAvatarUrl();
        try {
            userRepository.delete(user);
            // flush ngay để lỗi khoá ngoại nổ ở đây (bắt được), không phải lúc commit sau khi rời method
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // Nhân viên đã gắn với dữ liệu khác (đơn đã xử lý, nhật ký...) → giữ lại, chỉ khoá
            throw new AppException(ErrorCode.STAFF_HAS_RELATED_DATA);
        }
        deleteFileAfterCommit(avatarUrl);
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================

    /** Chỉ tìm trong ADMIN + STAFF: truyền id khách hàng vào cũng báo "không tìm thấy" */
    private User findStaff(Integer userId) {
        return userRepository.findByUserIdAndRoleIn(userId, STAFF_ROLES)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }

    /** Trang này chỉ tạo / đổi sang ADMIN hoặc STAFF, không biến nhân viên thành khách hàng */
    private static void checkStaffRole(Role role) {
        if (role != Role.ADMIN && role != Role.STAFF) throw new AppException(ErrorCode.INVALID_STAFF_ROLE);
    }

    /** Hệ thống luôn phải còn ≥ 1 ADMIN đang hoạt động, không thì không ai vào được trang quản trị nữa */
    private void ensureAnotherActiveAdmin() {
        if (userRepository.countActiveByRole(Role.ADMIN) <= 1) throw new AppException(ErrorCode.LAST_ADMIN);
    }

    private static String normalizeEmail(String email) { return email.trim().toLowerCase(); }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private void deleteFileAfterCommit(String url) {
        if (url == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { fileStorageService.deleteAvatar(url); }
        });
    }

    private StaffResponse toResponse(User u, Integer currentUserId) {
        return StaffResponse.builder()
                .userId(u.getUserId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .address(u.getAddress())
                .dateOfBirth(u.getDateOfBirth())
                .gender(u.getGender())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .isActive(u.isActive())
                .lastLogin(u.getLastLogin())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .self(Objects.equals(u.getUserId(), currentUserId))
                .build();
    }
}
