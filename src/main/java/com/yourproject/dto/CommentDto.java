package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;
    private UserSlimDto user; // User who commented
    private String text;
    private LocalDateTime commentedAt;
    // private Long announcementId; // If needed, but usually part of AnnouncementDto's list
}
