package com.example.tour.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // ← field null sẽ không hiển thị trong JSON
public class ApiResponse<T> {
    private boolean success;
    private int code;
    private String message;
    private T data;
    private List<String> errors;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(201)
                .message(message)
                .data(data)
                .build();
    }

    // bắt 1 lỗi
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();  // data, errors null → tự ẩn nhờ @JsonInclude
    }

    // bắt nhiều lỗi
    public static <T> ApiResponse<T> error(int code, String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .errors(errors)
                .build(); // data null → tự ẩn nhờ @JsonInclude
    }

}
