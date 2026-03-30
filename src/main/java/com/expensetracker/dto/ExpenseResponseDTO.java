package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponseDTO {
    private Long id;
    private String merchantName;
    private String category;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private String currency;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String notes;
    private String imageFileName;
    private LocalDateTime createdAt;
    private List<ExpenseDTOs.ExpenseItemDTO> items;
}