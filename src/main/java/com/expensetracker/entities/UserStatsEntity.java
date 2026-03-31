package com.expensetracker.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsEntity {

    // Singleton row — always id = 1
    @Id
    private Long id;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAccumulatedSavings = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDeficit = BigDecimal.ZERO;

    // Derived: totalAccumulatedSavings - totalDeficit
    public BigDecimal getNetSavings() {
        BigDecimal savings = totalAccumulatedSavings != null ? totalAccumulatedSavings : BigDecimal.ZERO;
        BigDecimal deficit = totalDeficit != null ? totalDeficit : BigDecimal.ZERO;
        return savings.subtract(deficit);
    }

    public BigDecimal getTotalDeficit() {
        return totalDeficit != null ? totalDeficit : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAccumulatedSavings() {
        return totalAccumulatedSavings != null ? totalAccumulatedSavings : BigDecimal.ZERO;
    }
}
