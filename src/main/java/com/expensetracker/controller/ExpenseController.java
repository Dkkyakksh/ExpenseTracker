package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.CreateExpenseRequestDTO;
import com.expensetracker.dto.ExpenseResponseDTO;
import com.expensetracker.dto.ExpenseSummaryDTO;
import com.expensetracker.dto.ExpenseUpdateRequestDTO;
import com.expensetracker.exception.AppExceptions.FileValidationException;
import com.expensetracker.exception.ErrorCode;
import com.expensetracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping(value="create-expense")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> createExpense(
            @Validated @RequestBody CreateExpenseRequestDTO request) {
        ExpenseResponseDTO expense = expenseService.createExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Expense created successfully", expense));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> uploadReceipt(
            @RequestParam("file") MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new FileValidationException(ErrorCode.FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new FileValidationException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }

        ExpenseResponseDTO expense = expenseService.uploadAndParseReceipt(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Receipt parsed and saved successfully", expense));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getAllExpenses() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getAllExpenses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpenseById(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> updateExpense(
            @PathVariable Long id,
            @RequestBody ExpenseUpdateRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.updateExpense(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.ok("Expense deleted successfully", null));
    }

    // ─── Filtering ────────────────────────────────────────────────────────────

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpensesByCategory(category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getAllCategories()));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ExpenseResponseDTO>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getExpensesByDateRange(start, end)));
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryDTO>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getSummary()));
    }

    @GetMapping("/summary/date-range")
    public ResponseEntity<ApiResponse<ExpenseSummaryDTO>> getSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.getSummaryByDateRange(start, end)));
    }
}
