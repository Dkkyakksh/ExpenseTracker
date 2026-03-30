package com.expensetracker.dto;

import lombok.*;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponseDTO {
    private String message;
    private ExpenseResponseDTO expense;
}
