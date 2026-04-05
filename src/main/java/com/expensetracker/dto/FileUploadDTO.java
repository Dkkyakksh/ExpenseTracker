package com.expensetracker.dto;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadDTO {
    String imageId;
    String status;
    String errorMessage;
}
