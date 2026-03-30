package com.expensetracker.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantSummaryDTO {
    private String merchantName;
    private BigDecimal totalAmount;
}