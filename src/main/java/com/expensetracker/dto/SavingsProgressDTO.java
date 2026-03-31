package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsProgressDTO {
    private BigDecimal currentMonthBalance;
    private BigDecimal totalAccumulatedSavings;
    private BigDecimal totalDeficit;
    private BigDecimal netSavings;              // totalAccumulatedSavings - totalDeficit
    private List<MonthlyBreakdownDTO> history;
}
