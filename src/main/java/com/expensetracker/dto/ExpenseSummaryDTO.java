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
    private List<CategorySummaryDTO> byCategory;
    private List<MerchantSummaryDTO> byMerchant;
}