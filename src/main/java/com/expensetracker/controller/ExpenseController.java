package com.expensetracker.controller;

import com.expensetracker.dto.*;
import com.expensetracker.entities.ReceiptUploadEntity;
import com.expensetracker.entities.ReceiptUploadEntity.UploadStatus;
import com.expensetracker.exception.AppExceptions.FileValidationException;
import com.expensetracker.exception.ErrorCode;
import com.expensetracker.repository.ReceiptUploadRepository;
import com.expensetracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ReceiptUploadRepository receiptUploadRepository;

    @PostMapping(value="create-expense")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> createExpense(
            @Validated @RequestBody CreateExpenseRequestDTO request) {
        ExpenseResponseDTO expense = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Expense created successfully", expense));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<FileUploadDTO>>> uploadReceipt(
            @RequestParam("files") List<MultipartFile> inputFiles) {

        String requestId = UUID.randomUUID().toString();
        if (CollectionUtils.isEmpty(inputFiles)) {
            throw new FileValidationException(ErrorCode.FILES_ARRAY_EMPTY);
        }

        List<FileUploadDTO> results = new ArrayList<>();
        for (MultipartFile inputFile : inputFiles) {
            String imageId = UUID.randomUUID().toString();

            if (inputFile == null || inputFile.isEmpty()) {
                results.add(new FileUploadDTO(imageId, "FAILED", ErrorCode.FILE_EMPTY.getDefaultMessage(), null, null, LocalDateTime.now()));
                continue;
            }

            String contentType = inputFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                results.add(new FileUploadDTO(imageId, "FAILED", ErrorCode.FILE_TYPE_UNSUPPORTED.getDefaultMessage(), null, null, LocalDateTime.now()));
                continue;
            }

            // Persist PROCESSING record before firing async job
            receiptUploadRepository.save(ReceiptUploadEntity.builder()
                    .imageId(imageId)
                    .requestId(requestId)
                    .status(UploadStatus.PROCESSING)
                    .build());

            expenseService.uploadAndParseReceipt(requestId, imageId, inputFile);
            results.add(new FileUploadDTO(imageId, "PROCESSING", null, null, null, LocalDateTime.now()));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(requestId, "Receipt parse request received successfully", results));
    }

    @GetMapping("/upload/{requestId}/status")
    public ResponseEntity<ApiResponse<List<FileUploadDTO>>> getUploadStatus(@PathVariable String requestId) {
        String respId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(respId, expenseService.getUploadStatusByRequestId(requestId)));
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getAllExpenses() {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getAllExpenses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> getExpenseById(@PathVariable Long id) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getExpenseById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> updateExpense(
            @PathVariable Long id,
            @RequestBody ExpenseUpdateRequestDTO request) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.updateExpense(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        String requestId = UUID.randomUUID().toString();
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.ok(requestId, "Expense deleted successfully", null));
    }

    // ─── Filtering ────────────────────────────────────────────────────────────

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getByCategory(@PathVariable String category) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getExpensesByCategory(category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getAllCategories()));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getExpensesByDateRange(start, end)));
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryDTO>> getSummary() {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getSummary()));
    }

    @GetMapping("/summary/date-range")
    public ResponseEntity<ApiResponse<ExpenseSummaryDTO>> getSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.ok(requestId, expenseService.getSummaryByDateRange(start, end)));
    }
}
