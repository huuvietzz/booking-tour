package com.example.tour.user.service.impl;

import com.example.tour.booking.repository.BookingRepository;
import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.user.constant.Role;
import com.example.tour.user.dto.request.CustomerCreateRequest;
import com.example.tour.user.dto.request.CustomerFilterRequest;
import com.example.tour.user.dto.request.CustomerUpdateRequest;
import com.example.tour.user.dto.response.CustomerResponse;
import com.example.tour.user.entity.User;
import com.example.tour.user.repository.UserRepository;
import com.example.tour.user.repository.UserTokenRepository;
import com.example.tour.user.service.CustomerService;
import com.example.tour.user.specification.CustomerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final UserRepository userRepository;
    private final UserTokenRepository tokenRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    // =====================================================================
    // XEM
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(CustomerFilterRequest filter, Pageable pageable) {
        Page<User> page = userRepository.findAll(CustomerSpecification.adminFilter(filter), pageable);

        // Số đơn của CẢ TRANG lấy bằng 1 câu GROUP BY, không đếm từng khách (10 khách = 10 câu → N+1)
        List<Integer> ids = page.getContent().stream().map(User::getUserId).toList();
        Map<Integer, Long> bookingCounts = ids.isEmpty() ? Map.of()
                : bookingRepository.countByUserIds(ids).stream()
                .collect(Collectors.toMap(r -> (Integer) r[0], r -> (Long) r[1]));

        return page.map(u -> toResponse(u, bookingCounts.getOrDefault(u.getUserId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Integer userId) {
        User user = findCustomer(userId);
        return toResponse(user, bookingRepository.countByUser_UserId(userId));
    }

    // =====================================================================
    // THÊM / SỬA
    // =====================================================================

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(blankToNull(request.getAddress()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        // Server tự quyết, không nhận từ client:
        user.setRole(Role.CUSTOMER);
        user.setActive(request.getIsActive() == null || request.getIsActive());
        // Tài khoản do nhân viên tạo hộ (đã xác minh qua điện thoại / quầy) → không bắt khách xác thực email nữa
        user.setEmailVerified(true);

        return toResponse(userRepository.save(user), 0);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer userId, CustomerUpdateRequest request) {
        User user = findCustomer(userId);

        // Email, SĐT là UNIQUE: chỉ kiểm khi thực sự đổi, trùng → báo lỗi rõ ràng thay vì 500 từ DB
        String email = normalizeEmail(request.getEmail());
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (!request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // Lưu ý: JWT dùng email làm subject → đổi email thì khách đang đăng nhập sẽ bị 401, phải đăng nhập lại bằng email mới
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(blankToNull(request.getAddress()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setActive(request.getIsActive() == null || request.getIsActive());
        // password, role, emailVerified KHÔNG sửa ở đây

        return toResponse(user, bookingRepository.countByUser_UserId(userId));   // entity đang được quản lý → commit tự UPDATE
    }

    /** Khoá / mở khoá: khoá rồi thì đăng nhập bị chặn (isAccountNonLocked = isActive) */
    @Override
    @Transactional
    public CustomerResponse setActive(Integer userId, boolean active) {
        User user = findCustomer(userId);
        user.setActive(active);
        return toResponse(user, bookingRepository.countByUser_UserId(userId));
    }

    // =====================================================================
    // XOÁ
    // =====================================================================

    /**
     * Chỉ xoá được khách CHƯA có đơn: đơn đặt tour / thanh toán là lịch sử kinh doanh, phải giữ.
     * Khách đã có đơn mà không muốn cho dùng nữa → KHOÁ tài khoản.
     */
    @Override
    @Transactional
    public void deleteCustomer(Integer userId) {
        User user = findCustomer(userId);
        if (bookingRepository.existsByUser_UserId(userId)) throw new AppException(ErrorCode.CUSTOMER_HAS_BOOKINGS);

        // Xoá trước các dòng con trỏ khoá ngoại tới users, không thì DB chặn xoá
        tokenRepository.deleteByUser_UserId(userId);
        // ⚠ Có bảng khác trỏ tới users (favorites, reviews...) thì xoá thêm ở đây, VD:
        // favoriteRepository.deleteByUser_UserId(userId);

        String avatarUrl = user.getAvatarUrl();
        userRepository.delete(user);
        deleteFileAfterCommit(avatarUrl);
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================

    /** Chỉ tìm trong khách hàng: truyền id của nhân viên / admin vào đây cũng báo "không tìm thấy" */
    private User findCustomer(Integer userId) {
        return userRepository.findByUserIdAndRole(userId, Role.CUSTOMER)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private static String normalizeEmail(String email) { return email.trim().toLowerCase(); }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    /** Xoá file ảnh SAU khi DB commit: rollback mà đã xoá file thì DB còn trỏ tới ảnh không tồn tại */
    private void deleteFileAfterCommit(String url) {
        if (url == null) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { fileStorageService.deleteAvatar(url); }
        });
    }

    private CustomerResponse toResponse(User u, long bookingCount) {
        return CustomerResponse.builder()
                .userId(u.getUserId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .address(u.getAddress())
                .dateOfBirth(u.getDateOfBirth())
                .gender(u.getGender())
                .avatarUrl(u.getAvatarUrl())
                .isActive(u.isActive())
                .emailVerified(u.isEmailVerified())
                .lastLogin(u.getLastLogin())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .bookingCount(bookingCount)
                .build();
    }
}
