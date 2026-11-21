package com.example.tour.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 * JSON OpenAPI: http://localhost:8080/v3/api-docs
 *
 * - Nút "Authorize" (ổ khoá): dán JWT (KHÔNG gõ chữ "Bearer ", Swagger tự thêm) → mọi API gửi kèm token.
 * - Chia nhóm Public / User / Admin: chọn ở ô "Select a definition" góc phải trên.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI viettripOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VietTrip API")
                        .version("1.0")
                        .description("API hệ thống đặt tour du lịch VietTrip")
                        .contact(new Contact().name("VietTrip").email("lienhe@viettrip.vn")))
                // Khai báo kiểu xác thực JWT (Authorization: Bearer <token>)
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // Áp cho mọi API: có token thì gửi kèm; API public không cần token vẫn gọi được bình thường
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder().group("1. Public").pathsToMatch("/api/public/**", "/api/auth/**").build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder().group("2. User").pathsToMatch("/api/**")
                .pathsToExclude("/api/public/**", "/api/auth/**", "/api/admin/**").build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder().group("3. Admin").pathsToMatch("/api/admin/**").build();
    }
}
