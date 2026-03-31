package com.expensetracker.repository;

import com.expensetracker.entities.UserStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStatsEntity, Long> {
    // Singleton row fetched by id = 1 via findById(1L)
}
