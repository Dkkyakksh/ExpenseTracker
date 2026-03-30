package com.expensetracker.service;

import com.expensetracker.dto.*;
import com.expensetracker.dto.ExpenseDTOs.*;
import com.expensetracker.model.Expense;
import com.expensetracker.model.ExpenseItem;
import com.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final GeminiService geminiService;

    // ─── Image Upload & Parse ─────────────────────────────────────────────────

    @Transactional
    public ExpenseResponseDTO uploadAndParseReceipt(MultipartFile imageFile) throws Exception {
        log.info("Processing receipt image: {}", imageFile.getOriginalFilename());

        // 1. Call Gemini to extract data from image
        GeminiParsedExpense parsed = geminiService.parseReceiptImage(imageFile);
        log.info("Gemini parsed merchant: {}, total: {}", parsed.getMerchantName(), parsed.getTotalAmount());

        // 2. Map parsed data to Expense entity
        Expense expense = mapParsedToExpense(parsed, imageFile.getOriginalFilename());

        // 3. Map line items
        if (parsed.getItems() != null) {
            List<ExpenseItem> items = parsed.getItems().stream()
                    .map(i -> ExpenseItem.builder()
                            .expense(expense)
                            .name(i.getName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .totalPrice(i.getTotalPrice() != null ? i.getTotalPrice() : BigDecimal.ZERO)
                            .build())
                    .collect(Collectors.toList());
            expense.getItems().addAll(items);
        }

        // 4. Save to database
        Expense saved = expenseRepository.save(expense);
        log.info("Saved expense with id: {}", saved.getId());

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
        Expense expense = findExpenseById(id);
        return toResponseDTO(expense);
    }

    @Transactional
    public ExpenseResponseDTO updateExpense(Long id, ExpenseUpdateRequestDTO request) {
        Expense expense = findExpenseById(id);

        if (request.getMerchantName() != null) expense.setMerchantName(request.getMerchantName());
        if (request.getCategory() != null) expense.setCategory(request.getCategory());
        if (request.getTotalAmount() != null) expense.setTotalAmount(request.getTotalAmount());
        if (request.getTaxAmount() != null) expense.setTaxAmount(request.getTaxAmount());
        if (request.getCurrency() != null) expense.setCurrency(request.getCurrency());
        if (request.getExpenseDate() != null) expense.setExpenseDate(request.getExpenseDate());
        if (request.getPaymentMethod() != null) expense.setPaymentMethod(request.getPaymentMethod());
        if (request.getNotes() != null) expense.setNotes(request.getNotes());

        return toResponseDTO(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = findExpenseById(id);
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

        List<CategorySummaryDTO> byCategory = expenseRepository.sumAmountGroupedByCategory()
                .stream()
                .map(row -> new CategorySummaryDTO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());

        return ExpenseSummaryDTO.builder()
                .totalSpend(total)
                .totalExpenses(expenses.size())
                .byCategory(byCategory)
                .byMerchant(List.of())
                .build();
    }

    // ─── Mapping Helpers ──────────────────────────────────────────────────────

    private Expense mapParsedToExpense(GeminiParsedExpense parsed, String fileName) {
        return Expense.builder()
                .merchantName(parsed.getMerchantName() != null ? parsed.getMerchantName() : "Unknown Merchant")
                .category(parsed.getCategory())
                .totalAmount(parsed.getTotalAmount() != null ? parsed.getTotalAmount() : BigDecimal.ZERO)
                .taxAmount(parsed.getTaxAmount())
                .currency(parsed.getCurrency() != null ? parsed.getCurrency() : "INR")
                .expenseDate(parseDate(parsed.getExpenseDate()))
                .paymentMethod(parsed.getPaymentMethod())
                .rawExtractedText(parsed.getRawText())
                .imageFileName(fileName)
                .build();
    }

    private ExpenseResponseDTO toResponseDTO(Expense expense) {
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

    private Expense findExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + id));
    }
}
