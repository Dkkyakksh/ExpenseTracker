package com.expensetracker.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_budgets", uniqueConstraints = @UniqueConstraint(columnNames = "month"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyBudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stored as first day of the month e.g. 2026-03-01
    @Column(nullable = false, unique = true)
    private LocalDate month;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal plannedSalary;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRolledOver = false;
}
