package com.expensetracker.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // General
    INTERNAL_ERROR("ERR_001", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ARGUMENT("ERR_002", "Invalid argument provided", HttpStatus.BAD_REQUEST),

    // Expense
    EXPENSE_NOT_FOUND("EXP_001", "Expense not found", HttpStatus.NOT_FOUND),
    EXPENSE_DELETE_FAILED("EXP_002", "Failed to delete expense", HttpStatus.INTERNAL_SERVER_ERROR),

    // File / Upload
    FILE_EMPTY("FILE_001", "file provided is empty", HttpStatus.BAD_REQUEST),
    FILE_TYPE_UNSUPPORTED("FILE_002", "Only image files are supported (jpeg, png, webp, gif)", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    FILE_TOO_LARGE("FILE_003", "File size exceeds the 10MB limit", HttpStatus.PAYLOAD_TOO_LARGE),
    FILES_ARRAY_EMPTY("FILE_004", "No Files provided", HttpStatus.BAD_REQUEST),

    // Gemini / AI parsing
    GEMINI_PARSE_FAILED("AI_001", "Failed to parse receipt with AI", HttpStatus.BAD_GATEWAY),
    GEMINI_EMPTY_RESPONSE("AI_002", "AI returned an empty response", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}
