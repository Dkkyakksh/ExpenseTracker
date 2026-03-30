package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseDTOs.*;
import com.expensetracker.dto.ExpenseResponseDTO;
import com.expensetracker.dto.ExpenseSummaryDTO;
import com.expensetracker.dto.ExpenseUpdateRequestDTO;
import com.expensetracker.dto.ImageUploadResponseDTO;
import com.expensetracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService expenseService;

    // ─── Receipt Image Upload ──────────────────────────────────────────────────

    /**
     * POST /api/expenses/upload
     * Upload a receipt image → Gemini parses it → saved to DB → returns expense
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponseDTO> uploadReceipt(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ImageUploadResponseDTO.builder()
                            .message("No file provided")
                            .build()
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(
                    ImageUploadResponseDTO.builder()
                            .message("Only image files are supported (jpeg, png, webp, gif)")
                            .build()
            );
        }

        try {
            ExpenseResponseDTO expense = expenseService.uploadAndParseReceipt(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ImageUploadResponseDTO.builder()
                            .message("Receipt parsed and saved successfully")
                            .expense(expense)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error processing receipt: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ImageUploadResponseDTO.builder()
                            .message("Failed to process receipt: " + e.getMessage())
                            .build()
            );
        }
    }

    // ─── CRUD Endpoints ───────────────────────────────────────────────────────

    /**
     * GET /api/expenses
     * List all expenses (most recent first)
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponseDTO>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    /**
     * GET /api/expenses/{id}
     * Get a single expense by ID (includes line items)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> getExpenseById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(expenseService.getExpenseById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/expenses/{id}
     * Update editable fields of an expense (corrections after Gemini parse)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ExpenseResponseDTO> updateExpense(
            @PathVariable Long id,
            @RequestBody ExpenseUpdateRequestDTO request) {
        try {
            return ResponseEntity.ok(expenseService.updateExpense(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/expenses/{id}
     * Delete an expense and all its line items
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteExpense(@PathVariable Long id) {
        try {
            expenseService.deleteExpense(id);
            return ResponseEntity.ok(Map.of("message", "Expense deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─── Filtering ────────────────────────────────────────────────────────────

    /**
     * GET /api/expenses/category/{category}
     * Filter expenses by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ExpenseResponseDTO>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(category));
    }

    /**
     * GET /api/expenses/categories
     * List all distinct categories in use
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(expenseService.getAllCategories());
    }

    /**
     * GET /api/expenses/date-range?start=YYYY-MM-DD&end=YYYY-MM-DD
     * Filter expenses between two dates
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ExpenseResponseDTO>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(start, end));
    }

    // ─── Analytics ────────────────────────────────────────────────────────────

    /**
     * GET /api/expenses/summary
     * Overall summary: total spend, count, breakdown by category and merchant
     */
    @GetMapping("/summary")
    public ResponseEntity<ExpenseSummaryDTO> getSummary() {
        return ResponseEntity.ok(expenseService.getSummary());
    }

    /**
     * GET /api/expenses/summary/date-range?start=YYYY-MM-DD&end=YYYY-MM-DD
     * Summary filtered by date range
     */
    @GetMapping("/summary/date-range")
    public ResponseEntity<ExpenseSummaryDTO> getSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(expenseService.getSummaryByDateRange(start, end));
    }
}
