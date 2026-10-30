package com.example.tour.user.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra validation của ChangePasswordRequest (các @AssertTrue, @Size...).
 * UserService KHÔNG tự kiểm tra "nhập lại khớp" hay "mật khẩu mới khác cũ":
 * hai quy tắc này nằm ở DTO, chạy nhờ @Valid ở controller → phải test riêng ở đây.
 */
class ChangePasswordRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static ChangePasswordRequest request(String current, String newPass, String confirm) {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword(current);
        req.setNewPassword(newPass);
        req.setConfirmPassword(confirm);
        return req;
    }

    private static Set<String> messages(ChangePasswordRequest req) {
        return validator.validate(req).stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("Hợp lệ → không có lỗi")
    void valid() {
        assertThat(messages(request("Cu@1234", "Moi@1234", "Moi@1234"))).isEmpty();
    }

    @Test
    @DisplayName("Nhập lại không khớp → báo lỗi")
    void confirmMismatch() {
        assertThat(messages(request("Cu@1234", "Moi@1234", "Khac@1234")))
                .contains("Mật khẩu nhập lại không khớp");
    }

    @Test
    @DisplayName("Mật khẩu mới trùng mật khẩu cũ → báo lỗi")
    void sameAsCurrent() {
        assertThat(messages(request("Cu@1234", "Cu@1234", "Cu@1234")))
                .contains("Mật khẩu mới phải khác mật khẩu hiện tại");
    }

    @Test
    @DisplayName("Mật khẩu mới dưới 6 ký tự → báo lỗi")
    void tooShort() {
        assertThat(messages(request("Cu@1234", "123", "123")))
                .contains("Mật khẩu phải có ít nhất 6 ký tự");
    }

    @Test
    @DisplayName("Bỏ trống mật khẩu hiện tại → báo lỗi")
    void blankCurrent() {
        assertThat(messages(request("", "Moi@1234", "Moi@1234")))
                .contains("Vui lòng nhập mật khẩu hiện tại");
    }
}