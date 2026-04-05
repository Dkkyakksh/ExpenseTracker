package com.expensetracker.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptUploadEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String imageId;

    @Column(nullable = false)
    private String requestId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    // Raw JSON blob from Gemini
    @Column(columnDefinition = "TEXT")
    private String extractedData;

    // User-corrected JSON blob (same structure as extractedData)
    @Column(columnDefinition = "TEXT")
    private String correctedData;

    // Error message if status = FAILED
    @Column(length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum UploadStatus {
        PROCESSING, COMPLETED, CONFIRMED, FAILED
    }
}
