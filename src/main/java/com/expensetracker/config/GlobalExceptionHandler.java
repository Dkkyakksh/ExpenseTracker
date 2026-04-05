package com.expensetracker.config;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.exception.BaseException;
import com.expensetracker.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@Order(1)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles all typed app exceptions (ExpenseNotFoundException, FileValidationException, etc.)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(String requestId, BaseException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[{}] {}", errorCode.getCode(), ex.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(requestId, ex.getMessage(), errorCode.getCode()));
    }

    // Spring multipart size limit exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(String requestId, MaxUploadSizeExceededException ex) {
        ErrorCode errorCode = ErrorCode.FILE_TOO_LARGE;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(requestId, errorCode.getDefaultMessage(), errorCode.getCode()));
    }

    // @Valid / @Validated bean validation failures — collects all field errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(String requestId, MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(requestId, message, ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    // Bad path variable or request param type (e.g. passing "abc" for a Long id)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(String requestId, MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(requestId, message, ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(String requestId, IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(requestId, ex.getMessage(), ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    // Catch-all — log full stack trace since this is unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(String requestId, Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(requestId, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), ErrorCode.INTERNAL_ERROR.getCode()));
    }
}
