package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDTO {
    private BigDecimal totalSpend;
    private long totalExpenses;
    private List<ExpenseDTOs.CategorySummaryDTO> byCategory;
    private List<ExpenseDTOs.MerchantSummaryDTO> byMerchant;
}