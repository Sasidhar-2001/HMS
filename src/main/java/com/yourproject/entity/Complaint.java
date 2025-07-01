package com.yourproject.entity;

import com.yourproject.entity.embeddable.ComplaintStatusHistoryItem;
import com.yourproject.entity.embeddable.ResolutionDetails;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaint_complaint_id_str", columnList = "complaintIdString", unique = true),
        @Index(name = "idx_complaint_reported_by", columnList = "reported_by_id"),
        @Index(name = "idx_complaint_assigned_to", columnList = "assigned_to_id"),
        @Index(name = "idx_complaint_status", columnList = "status"),
        @Index(name = "idx_complaint_category", columnList = "category"),
        @Index(name = "idx_complaint_priority", columnList = "priority"),
        @Index(name = "idx_complaint_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Complaint ID string is required for tracking") // This is the human-readable ID like CMP...
    @Column(name = "complaintIdString", unique = true, nullable = false, length = 50)
    private String complaintIdString;

    @NotBlank(message = "Complaint title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String title;

    @NotBlank(message = "Complaint description is required")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull(message = "Complaint category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @NotNull(message = "Reporter is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo; // e.g., a Warden

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room; // Room where the complaint originated, if applicable

    @Size(max = 255)
    @Column(length = 255)
    private String location; // Specific location if not tied to a room e.g. "Corridor Block A"

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "complaint_images", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "image_url", length = 255)
    private List<String> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "complaint_status_history", joinColumns = @JoinColumn(name = "complaint_id"))
    @OrderBy("updatedAt DESC")
    private List<ComplaintStatusHistoryItem> statusHistory = new ArrayList<>();

    @Embedded
    private ResolutionDetails resolution;

    private LocalDate expectedResolutionDate;
    private LocalDate actualResolutionDate;

    @Column(nullable = false)
    private boolean isUrgent = false; // Derived from priority in Mongoose, can be set in service

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "complaint_tags", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Mongoose pre-save for complaintIdString generation and isUrgent flag:
    // This logic will be in the ComplaintService.

    // Mongoose methods (updateStatus):
    // This will be part of the ComplaintService.

    // Virtuals (resolutionTimeHours, isOverdue):
    // These will be calculated in DTOs or service layer.
}
