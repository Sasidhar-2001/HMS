package com.yourproject.dto;

import com.yourproject.entity.AnnouncementPriority;
import com.yourproject.entity.AnnouncementStatus;
import com.yourproject.entity.AnnouncementTargetAudience;
import com.yourproject.entity.AnnouncementType;
import com.yourproject.entity.embeddable.Attachment; // Using embeddable directly
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDto {
    private Long id;
    private String title;
    private String content;
    private AnnouncementType type;
    private AnnouncementPriority priority;
    private AnnouncementTargetAudience targetAudience;
    private Set<RoomSlimDto> targetRooms; // Simplified DTO for rooms
    private Set<UserSlimDto> targetUsers; // Simplified DTO for users
    private UserSlimDto createdBy;
    private LocalDateTime publishDate;
    private LocalDateTime expiryDate;
    private AnnouncementStatus status;
    private List<Attachment> attachments; // Using embeddable directly
    private Set<ReadReceiptDto> readReceipts;
    private Set<LikeDto> likes;
    private List<CommentDto> commentEntries;
    private Set<String> tags;
    private boolean isSticky;
    private boolean emailSent;
    private boolean smsSent;
    private boolean notificationSent;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields (can be added by a mapper or service)
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isActive; // Based on status and expiryDate
}
