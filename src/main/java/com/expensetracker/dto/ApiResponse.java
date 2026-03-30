package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;
    private final String timestamp;

    private ApiResponse(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now().toString();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, message, null, errorCode);
    }
}
