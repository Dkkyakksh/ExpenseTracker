package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String requestId;
    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;
    private final String timestamp;

    private ApiResponse(String requestId, boolean success, String message, T data, String errorCode) {
        this.requestId = requestId;
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = Instant.now().toString(); // ISO-8601 UTC e.g. 2026-03-31T10:15:30Z
    }

    public static <T> ApiResponse<T> ok(String requestId, T data) {
        return new ApiResponse<>(requestId, true, null, data, null);
    }

    public static <T> ApiResponse<T> ok(String requestId, String message, T data) {
        return new ApiResponse<>(requestId, true, null, data, null);
    }

    public static <T> ApiResponse<T> ok(String requestId, String message) {
        return new ApiResponse<>(requestId, true, message, null, null);
    }

    public static <T> ApiResponse<T> error(String requestId, String message, String errorCode) {
        return new ApiResponse<>(requestId, false, message, null, errorCode);
    }
}
