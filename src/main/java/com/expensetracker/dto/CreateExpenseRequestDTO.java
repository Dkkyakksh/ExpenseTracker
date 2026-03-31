package com.expensetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequestDTO {

    private String merchantName;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal totalAmount;

    private BigDecimal taxAmount;
    private String currency;
    private LocalDate expenseDate;
    private String paymentMethod;
    private String notes;
}
