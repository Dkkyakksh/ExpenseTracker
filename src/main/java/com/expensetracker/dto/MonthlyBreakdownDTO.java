package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBreakdownDTO {
    private String month;           // e.g. "2026-03"
    private BigDecimal salary;
    private BigDecimal actualExpenses;
    private BigDecimal savings;     // salary - actualExpenses (can be negative)
}
