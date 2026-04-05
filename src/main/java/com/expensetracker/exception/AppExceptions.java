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

    public static class ImageUploadNotFoundException extends BaseException {
        public ImageUploadNotFoundException(String imageId) {
            super(ErrorCode.IMAGE_UPLOAD_NOT_FOUND, "No upload record found for imageId: " + imageId);
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
