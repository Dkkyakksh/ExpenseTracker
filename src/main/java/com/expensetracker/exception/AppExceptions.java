package com.expensetracker.exception;

public class AppExceptions {

    public static class ExpenseNotFoundException extends BaseException {
        public ExpenseNotFoundException(Long id) {
            super(ErrorCode.EXPENSE_NOT_FOUND, "Expense not found with id: " + id);
        }
    }

    public static class FileValidationException extends BaseException {
        public FileValidationException(ErrorCode errorCode) {
            super(errorCode);
        }
    }

    public static class GeminiException extends BaseException {
        public GeminiException(ErrorCode errorCode) {
            super(errorCode);
        }

        public GeminiException(ErrorCode errorCode, Throwable cause) {
            super(errorCode, cause);
        }
    }
}
