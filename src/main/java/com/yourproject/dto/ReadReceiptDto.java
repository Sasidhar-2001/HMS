package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadReceiptDto {
    private Long id;
    private UserSlimDto user; // User who read
    private LocalDateTime readAt;
    // private Long announcementId; // If needed
}
