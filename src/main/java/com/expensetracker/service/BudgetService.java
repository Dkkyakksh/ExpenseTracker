package com.expensetracker.service;

import com.expensetracker.dto.MonthlyBreakdownDTO;
import com.expensetracker.dto.SavingsProgressDTO;
import com.expensetracker.dto.SetBudgetRequestDTO;
import com.expensetracker.entities.MonthlyBudgetEntity;
import com.expensetracker.entities.UserStatsEntity;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.MonthlyBudgetRepository;
import com.expensetracker.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final Long STATS_ID = 1L;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MonthlyBudgetRepository budgetRepository;
    private final UserStatsRepository userStatsRepository;
    private final ExpenseRepository expenseRepository;

    // ─── Set / Update Monthly Budget ─────────────────────────────────────────

    @Transactional
    public void setMonthlyBudget(SetBudgetRequestDTO request) {
        LocalDate monthStart = request.getMonth().atDay(1);
        MonthlyBudgetEntity budget = budgetRepository.findByMonth(monthStart)
                .orElse(MonthlyBudgetEntity.builder().month(monthStart).build());
        budget.setPlannedSalary(request.getPlannedSalary());
        budgetRepository.save(budget);
        log.info("Set budget for {}: {}", monthStart, request.getPlannedSalary());
    }

    // ─── Current Month Balance ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BigDecimal calculateCurrentMonthBalance() {
        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = current.atEndOfMonth();

        BigDecimal salary = budgetRepository.findByMonth(start)
                .map(MonthlyBudgetEntity::getPlannedSalary)
                .orElse(BigDecimal.ZERO);

        BigDecimal spent = expenseRepository.sumExpensesForMonth(start, end);
        return salary.subtract(spent);
    }

    // ─── Month-End Sweep ──────────────────────────────────────────────────────

    /**
     * Call this when a new month starts.
     * Finds all previous unrolled months, calculates their remaining balance,
     * adds it to totalAccumulatedSavings, and marks them as rolled over.
     */
    @Transactional
    public void performMonthEndSweep() {
        LocalDate today = LocalDate.now();
        List<MonthlyBudgetEntity> unrolled = budgetRepository.findUnrolledMonthsBefore(today.withDayOfMonth(1));

        if (unrolled.isEmpty()) {
            log.info("Month-end sweep: nothing to roll over");
            return;
        }

        UserStatsEntity stats = getOrCreateStats();
        BigDecimal totalRollover = BigDecimal.ZERO;

        for (MonthlyBudgetEntity budget : unrolled) {
            LocalDate start = budget.getMonth();
            LocalDate end = YearMonth.from(start).atEndOfMonth();
            BigDecimal spent = expenseRepository.sumExpensesForMonth(start, end);
            BigDecimal remaining = budget.getPlannedSalary().subtract(spent);

            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                totalRollover = totalRollover.add(remaining);
            } else {
                // Overspent — track the deficit magnitude (positive number)
                stats.setTotalDeficit(stats.getTotalDeficit().add(remaining.abs()));
                log.info("Deficit recorded for {}: {}", start, remaining);
            }

            budget.setRolledOver(true);
            log.info("Rolled over {}: remaining={}", start, remaining);
        }

        stats.setTotalAccumulatedSavings(stats.getTotalAccumulatedSavings().add(totalRollover));
        userStatsRepository.save(stats);
        budgetRepository.saveAll(unrolled);
        log.info("Sweep complete. Added {} to accumulated savings", totalRollover);
    }

    // ─── Savings Progress ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SavingsProgressDTO getSavingsProgress(int months) {
        LocalDate since = YearMonth.now().minusMonths(months - 1).atDay(1);
        List<MonthlyBudgetEntity> budgets = budgetRepository.findBudgetsSince(since);

        Map<String, MonthlyBudgetEntity> budgetMap = budgets.stream()
                .collect(Collectors.toMap(
                        b -> YearMonth.from(b.getMonth()).format(MONTH_FMT),
                        b -> b
                ));

        List<MonthlyBreakdownDTO> history = new ArrayList<>();

        // Walk through each of the last 12 months
        for (int i = months-1; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            String key = ym.format(MONTH_FMT);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal salary = budgetMap.containsKey(key)
                    ? budgetMap.get(key).getPlannedSalary()
                    : BigDecimal.ZERO;

            BigDecimal spent = expenseRepository.sumExpensesForMonth(start, end);
            BigDecimal savings = salary.subtract(spent);

            history.add(MonthlyBreakdownDTO.builder()
                    .month(key)
                    .salary(salary)
                    .actualExpenses(spent)
                    .savings(savings)
                    .build());
        }

        BigDecimal currentBalance = calculateCurrentMonthBalance();
        UserStatsEntity stats = getOrCreateStats();

        return SavingsProgressDTO.builder()
                .currentMonthBalance(currentBalance)
                .totalAccumulatedSavings(stats.getTotalAccumulatedSavings())
                .totalDeficit(stats.getTotalDeficit())
                .netSavings(stats.getNetSavings())
                .history(history)
                .build();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private UserStatsEntity getOrCreateStats() {
        UserStatsEntity stats = userStatsRepository.findById(STATS_ID)
                .orElseGet(() -> UserStatsEntity.builder()
                        .id(STATS_ID)
                        .totalAccumulatedSavings(BigDecimal.ZERO)
                        .totalDeficit(BigDecimal.ZERO)
                        .build());

        // Migrate existing rows that predate the totalDeficit column
        if (stats.getTotalDeficit() == null) {
            stats.setTotalDeficit(BigDecimal.ZERO);
            userStatsRepository.save(stats);
        }

        return stats;
    }
}