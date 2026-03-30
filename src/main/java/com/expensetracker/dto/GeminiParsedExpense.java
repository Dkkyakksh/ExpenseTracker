package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiParsedExpense {
    private String merchantName;
    private String category;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String currency;
    private String expenseDate;       // Gemini returns string; we parse it
    private String paymentMethod;
    private List<GeminiParsedItem> items;
    private String rawText;
}