package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseUpdateRequestDTO {
    private String merchantName;
    private String category;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String currency;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String notes;
}