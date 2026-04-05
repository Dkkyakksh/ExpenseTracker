package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadDTO {
    String imageId;
    String status;
    String errorMessage;
    Object extractedData;
    Object correctedData;
    LocalDateTime updatedAt;
}