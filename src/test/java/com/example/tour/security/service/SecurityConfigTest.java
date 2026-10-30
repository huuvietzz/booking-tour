package com.example.tour.security.service;

import com.example.tour.security.config.SecurityConfig;
import com.example.tour.security.filter.JwtAuthenticationFilter;
import com.example.tour.user.controller.AdminController;
import com.example.tour.user.controller.UserController;
import com.example.tour.user.service.AdminService;
import com.example.tour.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra QUY TẮC PHÂN QUYỀN trong SecurityConfig (ai được gọi URL nào).
 * Chỉ khởi động tầng web: controller + filter bảo mật. Service là mock → không cần DB.
 *
 * Chỉ cần quan tâm mã trạng thái: 401 (chưa đăng nhập), 403 (thiếu quyền),
 * khác 401/403 (đã qua lớp bảo mật).
 */
@WebMvcTest(controllers = { UserController.class, AdminController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class SecurityConfigTest {

    @Autowired MockMvc mvc;

    // Các bean mà controller / filter cần → giả lập
    @MockitoBean UserService userService;
    @MockitoBean AdminService adminService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    // ----- Chưa đăng nhập -----
    @Test
    @DisplayName("Chưa đăng nhập gọi API người dùng → 401")
    void anonymousUserApi() throws Exception {
        mvc.perform(get("/api/users/account-info")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Chưa đăng nhập gọi API admin → 401")
    void anonymousAdminApi() throws Exception {
        mvc.perform(get("/api/admin/account-info")).andExpect(status().isUnauthorized());
    }

    // ----- Khách hàng -----
    @Test
    @WithMockUser(username = "an@gmail.com", roles = "CUSTOMER")
    @DisplayName("Khách hàng gọi API admin → 403")
    void customerCannotAccessAdmin() throws Exception {
        mvc.perform(get("/api/admin/account-info")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "an@gmail.com", roles = "CUSTOMER")
    @DisplayName("Khách hàng gọi API hồ sơ của mình → qua được lớp bảo mật")
    void customerCanAccessOwnProfile() throws Exception {
        mvc.perform(get("/api/users/account-info")).andExpect(status().isOk());
    }

    // ----- Nhân viên -----
    @Test
    @WithMockUser(username = "son@gmail.com", roles = "STAFF")
    @DisplayName("Nhân viên vào trang admin chung → được phép")
    void staffCanAccessAdminArea() throws Exception {
        mvc.perform(get("/api/admin/account-info")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "son@gmail.com", roles = "STAFF")
    @DisplayName("Nhân viên gọi API quản lý nhân viên → 403 (chỉ ADMIN)")
    void staffCannotManageStaff() throws Exception {
        mvc.perform(get("/api/admin/staff")).andExpect(status().isForbidden());
    }

    // ----- Admin -----
    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    @DisplayName("Admin gọi API quản lý nhân viên → không bị chặn bởi phân quyền")
    void adminCanManageStaff() throws Exception {
        // Có thể là 404 nếu chưa có StaffController trong test này — miễn KHÔNG phải 401/403
        mvc.perform(get("/api/admin/staff"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    if (code == 401 || code == 403) throw new AssertionError("Admin bị chặn: " + code);
                });
    }
}