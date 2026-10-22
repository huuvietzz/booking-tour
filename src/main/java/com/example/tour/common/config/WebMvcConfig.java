package com.example.tour.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        if(!location.endsWith("/")) location += "/"; // thiếu "/" là Spring không tìm thấy file

        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(location);

        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadLocation = uploadRoot.toUri().toString();
        if (!uploadLocation.endsWith("/")) uploadLocation += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
