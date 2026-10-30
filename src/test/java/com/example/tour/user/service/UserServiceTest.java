package com.example.tour.user.service;

import com.example.tour.common.exception.AppException;
import com.example.tour.common.exception.ErrorCode;
import com.example.tour.common.service.FileStorageService;
import com.example.tour.common.util.SecurityUtils;
import com.example.tour.user.constant.Role;
import com.example.tour.user.dto.request.ChangePasswordRequest;
import com.example.tour.user.dto.request.UpdateProfileRequest;
import com.example.tour.user.dto.response.UserResponse;
import com.example.tour.user.entity.User;
import com.example.tour.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test UserService: KHÔNG khởi động Spring, KHÔNG dùng DB.
 * Repository, PasswordEncoder, FileStorageService là mock; SecurityUtils (static) được mockStatic.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final int CURRENT_USER_ID = 1;

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FileStorageService fileStorageService;

    @InjectMocks UserService userService;

    /** SecurityUtils.getCurrentUserId() là static → phải mock riêng, và PHẢI close sau mỗi test */
    private MockedStatic<SecurityUtils> securityUtils;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .userId(CURRENT_USER_ID)
                .email("an@gmail.com")
                .password("$2a$10$hashCu")
                .fullName("Nguyễn An")
                .phoneNumber("0900000001")
                .role(Role.CUSTOMER)
                .build();

        // Giả lập "đang đăng nhập với userId = 1"
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

        // lenient: test "không tìm thấy user" sẽ ghi đè stub này
        lenient().when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(currentUser));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Service đăng ký dọn file sau commit. Unit test không có transaction thật
        // → tự bật, nếu không registerSynchronization ném IllegalStateException.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();   // không close → các test sau vẫn bị mock static, kết quả sai lệch
        TransactionSynchronizationManager.clearSynchronization();
    }

    /** ChangePasswordRequest chỉ có setter → tạo bằng hàm hỗ trợ */
    private static ChangePasswordRequest changePasswordRequest(String current, String newPass, String confirm) {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(current);
        req.setNewPassword(newPass);
        req.setConfirmPassword(confirm);
        return req;
    }

    /** Giả lập kết thúc transaction: gọi afterCompletion của mọi synchronization đã đăng ký */
    private static void finishTransaction(int status) {
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        syncs.forEach(s -> s.afterCompletion(status));
    }

    // =====================================================================
    @Nested
    @DisplayName("Lấy hồ sơ")
    class GetProfile {

        @Test
        @DisplayName("User tồn tại → trả đúng thông tin")
        void success() {
            UserResponse res = userService.getMyProfile();

            assertThat(res.getUserId()).isEqualTo(CURRENT_USER_ID);
            assertThat(res.getEmail()).isEqualTo("an@gmail.com");
        }

        @Test
        @DisplayName("User không còn trong DB → USER_NOT_FOUND")
        void userNotFound() {
            when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyProfile())
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Đổi mật khẩu")
    class ChangePassword {

        @Test
        @DisplayName("Mật khẩu hiện tại sai → CURRENT_PASSWORD_INCORRECT, KHÔNG lưu")
        void wrongCurrentPassword() {
            ChangePasswordRequest req = changePasswordRequest("saiMatKhau", "Moi@1234", "Moi@1234");
            when(passwordEncoder.matches("saiMatKhau", "$2a$10$hashCu")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(req))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.CURRENT_PASSWORD_INCORRECT);

            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Đúng mật khẩu cũ → lưu mật khẩu MỚI đã mã hoá")
        void success() {
            ChangePasswordRequest req = changePasswordRequest("Cu@1234", "Moi@1234", "Moi@1234");
            when(passwordEncoder.matches("Cu@1234", "$2a$10$hashCu")).thenReturn(true);
            when(passwordEncoder.encode("Moi@1234")).thenReturn("$2a$10$hashMoi");

            userService.changePassword(req);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getPassword())
                    .isEqualTo("$2a$10$hashMoi")      // đã mã hoá
                    .isNotEqualTo("Moi@1234");         // KHÔNG lưu chữ thường
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Sửa hồ sơ")
    class UpdateProfile {

        private UpdateProfileRequest request(String fullName, String phone, String address) {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFullName(fullName);
            req.setPhoneNumber(phone);
            req.setAddress(address);
            return req;
        }

        @Test
        @DisplayName("Đổi sang SĐT đã có người dùng → PHONE_ALREADY_EXISTS, KHÔNG lưu")
        void duplicatePhone() {
            when(userRepository.existsByPhoneNumber("0911111111")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateMyProfile(request("Nguyễn An", "0911111111", null)))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Giữ nguyên SĐT cũ → KHÔNG kiểm tra trùng (không tự báo trùng với chính mình)")
        void samePhoneSkipsDuplicateCheck() {
            userService.updateMyProfile(request("Nguyễn Văn An", "0900000001", "Hà Nội"));

            verify(userRepository, never()).existsByPhoneNumber(anyString());
        }

        @Test
        @DisplayName("Hợp lệ → cập nhật đúng field, email KHÔNG đổi")
        void success() {
            UserResponse res = userService.updateMyProfile(request("Nguyễn Văn An", "0922222222", "Hà Nội"));

            assertThat(res.getFullName()).isEqualTo("Nguyễn Văn An");
            assertThat(res.getPhoneNumber()).isEqualTo("0922222222");
            assertThat(res.getAddress()).isEqualTo("Hà Nội");
            assertThat(res.getEmail()).isEqualTo("an@gmail.com");
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Ảnh đại diện")
    class Avatar {

        private final MockMultipartFile file =
                new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        @Test
        @DisplayName("Đổi ảnh → lưu URL mới, ảnh cũ CHƯA bị xoá trước khi commit")
        void replaceAvatarBeforeCommit() {
            currentUser.setAvatarUrl("/avatars/cu.jpg");
            when(fileStorageService.storeAvatar(file)).thenReturn("/avatars/moi.png");

            UserResponse res = userService.updateMyAvatar(file);

            assertThat(res.getAvatarUrl()).isEqualTo("/avatars/moi.png");
            verify(fileStorageService, never()).deleteAvatar(anyString());
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        }

        @Test
        @DisplayName("Commit thành công → xoá ảnh CŨ, giữ ảnh mới")
        void commitDeletesOldAvatar() {
            currentUser.setAvatarUrl("/avatars/cu.jpg");
            when(fileStorageService.storeAvatar(file)).thenReturn("/avatars/moi.png");

            userService.updateMyAvatar(file);
            finishTransaction(TransactionSynchronization.STATUS_COMMITTED);

            verify(fileStorageService).deleteAvatar("/avatars/cu.jpg");
            verify(fileStorageService, never()).deleteAvatar("/avatars/moi.png");
        }

        @Test
        @DisplayName("Rollback → xoá ảnh MỚI vừa lưu, giữ ảnh cũ")
        void rollbackDeletesNewAvatar() {
            currentUser.setAvatarUrl("/avatars/cu.jpg");
            when(fileStorageService.storeAvatar(file)).thenReturn("/avatars/moi.png");

            userService.updateMyAvatar(file);
            finishTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(fileStorageService).deleteAvatar("/avatars/moi.png");
            verify(fileStorageService, never()).deleteAvatar("/avatars/cu.jpg");
        }

        @Test
        @DisplayName("File không phải ảnh → lỗi truyền ra, KHÔNG lưu DB, KHÔNG hẹn xoá file")
        void invalidFile() {
            when(fileStorageService.storeAvatar(file)).thenThrow(new AppException(ErrorCode.INVALID_FILE_TYPE));

            assertThatThrownBy(() -> userService.updateMyAvatar(file))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE_TYPE);

            verify(userRepository, never()).save(any());
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        }

        @Test
        @DisplayName("Xoá ảnh → avatarUrl = null; commit xong mới xoá file cũ")
        void deleteAvatar() {
            currentUser.setAvatarUrl("/avatars/cu.jpg");

            UserResponse res = userService.deleteMyAvatar();
            assertThat(res.getAvatarUrl()).isNull();
            verify(fileStorageService, never()).deleteAvatar(anyString());

            finishTransaction(TransactionSynchronization.STATUS_COMMITTED);
            verify(fileStorageService).deleteAvatar("/avatars/cu.jpg");
        }
    }
}