package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.dto.CreateExpenseRequestDTO;
import com.expensetracker.exception.AppExceptions.ExpenseNotFoundException;
import com.expensetracker.exception.AppExceptions.FileValidationException;
import com.expensetracker.exception.AppExceptions.ImageUploadNotFoundException;
import com.expensetracker.exception.ErrorCode;
import com.expensetracker.entities.ExpenseEntity;
import com.expensetracker.entities.ExpenseItemEntity;
import com.expensetracker.entities.ReceiptUploadEntity;
import com.expensetracker.entities.ReceiptUploadEntity.UploadStatus;
import com.expensetracker.dto.CreateExpenseRequestDTO.Source;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.ReceiptUploadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ReceiptUploadRepository receiptUploadRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    // ─── Image Upload & Parse ─────────────────────────────────────────────────

    @Async
    public void uploadAndParseReceipt(String requestId, String imageId, MultipartFile imageFile) {
        log.info("[requestId: {}] Processing receipt image: {}", requestId, imageFile.getOriginalFilename());
        try {
            // 1. Call Gemini
            GeminiParsedExpense parsed = geminiService.parseReceiptImage(requestId, imageFile);
            log.info("[requestId: {}] Gemini parsed merchant: {}, total: {}", requestId, parsed.getMerchantName(), parsed.getTotalAmount());

            // 2. Stringify parsed data as JSON blob
            String extractedJson = objectMapper.writeValueAsString(parsed);

            // 3. Update upload record to COMPLETED with extracted data
            ReceiptUploadEntity upload = receiptUploadRepository.findById(imageId)
                    .orElseThrow(() -> new IllegalStateException("Upload record not found for imageId: " + imageId));
            upload.setStatus(UploadStatus.COMPLETED);
            upload.setExtractedData(extractedJson);
            receiptUploadRepository.save(upload);

            log.info("[requestId: {}] imageId: {} marked COMPLETED", requestId, imageId);
        } catch (Exception e) {
            log.error("[requestId: {}] Failed to process imageId: {}: {}", requestId, imageId, e.getMessage());
            receiptUploadRepository.findById(imageId).ifPresent(upload -> {
                upload.setStatus(UploadStatus.FAILED);
                upload.setErrorMessage(e.getMessage());
                receiptUploadRepository.save(upload);
            });
        }
    }

    // ─── Polling ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FileUploadDTO> getUploadStatusByRequestId(String requestId) {
        return receiptUploadRepository.findByRequestId(requestId).stream()
                .map(upload -> FileUploadDTO.builder()
                        .imageId(upload.getImageId())
                        .status(upload.getStatus().name())
                        .extractedData(parseJsonBlob(upload.getExtractedData()))
                        .correctedData(parseJsonBlob(upload.getCorrectedData()))
                        .errorMessage(upload.getErrorMessage())
                        .updatedAt(upload.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private Object parseJsonBlob(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON blob: {}", e.getMessage());
            return json;
        }
    }

    // ─── Manual Entry ─────────────────────────────────────────────────────────

    @Transactional
    public ExpenseResponseDTO createExpense(CreateExpenseRequestDTO request) {
        ExpenseEntity expense = ExpenseEntity.builder()
                .merchantName(normalize(request.getMerchantName()) != null ? normalize(request.getMerchantName()) : "manual")
                .category(normalize(request.getCategory()))
                .totalAmount(request.getTotalAmount())
                .taxAmount(request.getTaxAmount())
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase() : "INR")
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .paymentMethod(normalize(request.getPaymentMethod()))
                .notes(request.getNotes())
                .source(request.getSource())
                .build();

        // Map line items if present
        if (request.getItems() != null) {
            List<ExpenseItemEntity> items = request.getItems().stream()
                    .map(i -> ExpenseItemEntity.builder()
                            .expense(expense)
                            .name(i.getName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .totalPrice(i.getTotalPrice() != null ? i.getTotalPrice() : BigDecimal.ZERO)
                            .build())
                    .collect(Collectors.toList());
            expense.getItems().addAll(items);
        }

        // If source is IMAGE, validate imageId, store corrected data, and mark upload as CONFIRMED
        if (request.getSource() == Source.IMAGE) {
            if (request.getImageId() == null || request.getImageId().isBlank()) {
                throw new FileValidationException(ErrorCode.IMAGE_ID_REQUIRED);
            }
            expense.setImageId(request.getImageId());

            ReceiptUploadEntity upload = receiptUploadRepository.findById(request.getImageId())
                    .orElseThrow(() -> new ImageUploadNotFoundException(request.getImageId()));

            try {
                upload.setCorrectedData(objectMapper.writeValueAsString(request));
            } catch (Exception e) {
                log.warn("Failed to serialize corrected data for imageId: {}", request.getImageId());
            }
            upload.setStatus(UploadStatus.CONFIRMED);
            receiptUploadRepository.save(upload);
        }

        ExpenseEntity saved = expenseRepository.save(expense);
        log.info("[source: {}] Created expense with id: {}", request.getSource(), saved.getId());
        return toResponseDTO(saved);
    }

    // ─── CRUD Operations ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExpenseResponseDTO> getAllExpenses() {
        return expenseRepository.findAllByOrderByExpenseDateDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseResponseDTO getExpenseById(Long id) {
        ExpenseEntity expense = findExpenseById(id);
        return toResponseDTO(expense);
    }

    @Transactional
    public ExpenseResponseDTO updateExpense(Long id, ExpenseUpdateRequestDTO request) {
        ExpenseEntity expense = findExpenseById(id);

        if (request.getMerchantName() != null) expense.setMerchantName(normalize(request.getMerchantName()));
        if (request.getCategory() != null) expense.setCategory(normalize(request.getCategory()));
        if (request.getTotalAmount() != null) expense.setTotalAmount(request.getTotalAmount());
        if (request.getTaxAmount() != null) expense.setTaxAmount(request.getTaxAmount());
        if (request.getCurrency() != null) expense.setCurrency(request.getCurrency().toUpperCase());
        if (request.getExpenseDate() != null) expense.setExpenseDate(request.getExpenseDate());
        if (request.getPaymentMethod() != null) expense.setPaymentMethod(normalize(request.getPaymentMethod()));
        if (request.getNotes() != null) expense.setNotes(request.getNotes());

        return toResponseDTO(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id) {
        ExpenseEntity expense = findExpenseById(id);
        expenseRepository.delete(expense);
        log.info("Deleted expense id: {}", id);
    }

    // ─── Filtering ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExpenseResponseDTO> getExpensesByCategory(String category) {
        return expenseRepository.findByCategory(category)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponseDTO> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByExpenseDateBetween(startDate, endDate)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return expenseRepository.findAllDistinctCategories();
    }

    // ─── Analytics / Summary ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExpenseSummaryDTO getSummary() {
        BigDecimal total = expenseRepository.sumAllTotalAmounts();
        long count = expenseRepository.count();

        List<CategorySummaryDTO> byCategory = expenseRepository.sumAmountGroupedByCategory()
                .stream()
                .map(row -> new CategorySummaryDTO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        List<MerchantSummaryDTO> byMerchant = expenseRepository.sumAmountGroupedByMerchant()
                .stream()
                .map(row -> new MerchantSummaryDTO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        return ExpenseSummaryDTO.builder()
                .totalSpend(total)
                .totalExpenses(count)
                .byCategory(byCategory)
                .byMerchant(byMerchant)
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDTO getSummaryByDateRange(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.sumTotalAmountsBetweenDates(startDate, endDate);
        List<ExpenseResponseDTO> expenses = getExpensesByDateRange(startDate, endDate);

        List<CategorySummaryDTO> byCategory = expenseRepository.sumAmountGroupedByCategoryBetweenDates(startDate, endDate)
                .stream()
                .map(row -> new CategorySummaryDTO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        List<MerchantSummaryDTO> byMerchant = expenseRepository.sumAmountGroupedByMerchantBetweenDates(startDate, endDate)
                .stream()
                .map(row -> new MerchantSummaryDTO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        return ExpenseSummaryDTO.builder()
                .totalSpend(total)
                .totalExpenses(expenses.size())
                .byCategory(byCategory)
                .byMerchant(byMerchant)
                .build();
    }

    // ─── Mapping Helpers ──────────────────────────────────────────────────────

    private ExpenseEntity mapParsedToExpense(GeminiParsedExpense parsed, String fileName) {
        return ExpenseEntity.builder()
                .merchantName(parsed.getMerchantName() != null ? normalize(parsed.getMerchantName()) : "Unknown Merchant")
                .category(normalize(parsed.getCategory()))
                .totalAmount(parsed.getTotalAmount() != null ? parsed.getTotalAmount() : BigDecimal.ZERO)
                .taxAmount(parsed.getTaxAmount())
                .currency(parsed.getCurrency() != null ? parsed.getCurrency().toUpperCase() : "INR")
                .expenseDate(parseDate(parsed.getExpenseDate()))
                .paymentMethod(normalize(parsed.getPaymentMethod()))
                .rawExtractedText(parsed.getRawText())
                .imageFileName(fileName)
                .build();
    }

    private ExpenseResponseDTO toResponseDTO(ExpenseEntity expense) {
        List<ExpenseItemDTO> itemDTOs = expense.getItems().stream()
                .map(i -> ExpenseItemDTO.builder()
                        .id(i.getId())
                        .name(i.getName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .merchantName(expense.getMerchantName())
                .category(expense.getCategory())
                .totalAmount(expense.getTotalAmount())
                .taxAmount(expense.getTaxAmount())
                .currency(expense.getCurrency())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .notes(expense.getNotes())
                .imageFileName(expense.getImageFileName())
                .createdAt(expense.getCreatedAt())
                .items(itemDTOs)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("Could not parse date '{}', defaulting to today", dateStr);
            return LocalDate.now();
        }
    }

    private ExpenseEntity findExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    /** Trims and lowercases a string. Returns null if input is null. */
    private String normalize(String value) {
        return value != null ? value.trim().toLowerCase() : null;
    }
}
