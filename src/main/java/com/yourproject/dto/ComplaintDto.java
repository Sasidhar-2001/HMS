package com.yourproject.dto;

import com.yourproject.entity.ComplaintCategory;
import com.yourproject.entity.ComplaintPriority;
import com.yourproject.entity.ComplaintStatus;
import com.yourproject.entity.embeddable.ComplaintStatusHistoryItem;
import com.yourproject.entity.embeddable.ResolutionDetails;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDto {
    private Long id;
    private String complaintIdString; // Human-readable ID
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintPriority priority;
    private ComplaintStatus status;
    private UserSlimDto reportedBy;
    private UserSlimDto assignedTo;
    private RoomSlimDto room;
    private String location;
    private List<String> images; // URLs
    private List<ComplaintStatusHistoryItem> statusHistory; // Using embeddable directly
    private ResolutionDetails resolution; // Using embeddable directly
    private LocalDate expectedResolutionDate;
    private LocalDate actualResolutionDate;
    private boolean isUrgent;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields (can be added by mapper or service)
    private Integer resolutionTimeHours;
    private Boolean isOverdue;
}
