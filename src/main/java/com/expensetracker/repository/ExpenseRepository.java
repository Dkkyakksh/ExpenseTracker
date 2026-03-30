package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByMerchantNameContainingIgnoreCase(String merchantName);

    List<Expense> findByCategory(String category);

    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    List<Expense> findByCategoryAndExpenseDateBetween(String category, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e")
    BigDecimal sumAllTotalAmounts();

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.category = :category")
    BigDecimal sumTotalAmountsByCategory(@Param("category") String category);

    @Query("SELECT e.category, COALESCE(SUM(e.totalAmount), 0) FROM Expense e GROUP BY e.category ORDER BY SUM(e.totalAmount) DESC")
    List<Object[]> sumAmountGroupedByCategory();

    @Query("SELECT e.merchantName, COALESCE(SUM(e.totalAmount), 0) FROM Expense e GROUP BY e.merchantName ORDER BY SUM(e.totalAmount) DESC")
    List<Object[]> sumAmountGroupedByMerchant();

    @Query("SELECT DISTINCT e.category FROM Expense e WHERE e.category IS NOT NULL ORDER BY e.category")
    List<String> findAllDistinctCategories();

    List<Expense> findAllByOrderByExpenseDateDesc();
}
