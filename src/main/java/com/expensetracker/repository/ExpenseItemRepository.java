package com.expensetracker.repository;

import com.expensetracker.entities.ExpenseItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseItemRepository extends JpaRepository<ExpenseItemEntity, Long> {
    List<ExpenseItemEntity> findByExpenseId(Long expenseId);
}
