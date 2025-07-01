package com.yourproject.dto;

import com.yourproject.entity.AnnouncementPriority;
import com.yourproject.entity.AnnouncementStatus;
import com.yourproject.entity.AnnouncementTargetAudience;
import com.yourproject.entity.AnnouncementType;
import com.yourproject.entity.embeddable.Attachment; // Can be simplified for request if needed
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 2000)
    private String content;

    @NotNull
    private AnnouncementType type = AnnouncementType.GENERAL;

    @NotNull
    private AnnouncementPriority priority = AnnouncementPriority.MEDIUM;

    @NotNull
    private AnnouncementTargetAudience targetAudience = AnnouncementTargetAudience.ALL;

    private Set<Long> targetRoomIds; // IDs for rooms
    private Set<Long> targetUserIds; // IDs for users

    // createdBy will be set from the authenticated user in the service layer

    private LocalDateTime publishDate; // Optional: if not set and status is PUBLISHED, defaults to now
    private LocalDateTime expiryDate;  // Optional

    @NotNull
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    // For attachments, client might send file metadata or new files to upload.
    // This DTO part might need to align with how file uploads are handled (e.g., separate endpoint or multipart form).
    // For simplicity, let's assume attachment info is provided if files are already uploaded,
    // or this list might be ignored if new files are part of a multipart request.
    private List<Attachment> attachments;

    private Set<String> tags;
    private Boolean isSticky;

    // emailSent, smsSent, notificationSent are usually server-side status flags, not part of request.
    // viewCount is also server-managed.
}
