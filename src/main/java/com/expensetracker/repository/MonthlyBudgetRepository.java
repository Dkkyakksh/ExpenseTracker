package com.expensetracker.repository;

import com.expensetracker.entities.MonthlyBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudgetEntity, Long> {

    Optional<MonthlyBudgetEntity> findByMonth(LocalDate month);

    // Fetch last N months of budgets ordered newest first
    @Query("SELECT b FROM MonthlyBudgetEntity b WHERE b.month >= :since ORDER BY b.month DESC")
    List<MonthlyBudgetEntity> findBudgetsSince(@Param("since") LocalDate since);

    // All unrolled months before a given date (used for sweep)
    @Query("SELECT b FROM MonthlyBudgetEntity b WHERE b.month < :month AND b.isRolledOver = false")
    List<MonthlyBudgetEntity> findUnrolledMonthsBefore(@Param("month") LocalDate month);
}
