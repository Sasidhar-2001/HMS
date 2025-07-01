package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeDto {
    private Long id;
    private UserSlimDto user; // User who liked
    private LocalDateTime likedAt;
    // private Long announcementId; // If needed
}
