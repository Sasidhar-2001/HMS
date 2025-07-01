package com.yourproject.entity;

import com.yourproject.entity.embeddable.Attachment;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_created_by", columnList = "created_by_id"),
        @Index(name = "idx_announcement_status", columnList = "status"),
        @Index(name = "idx_announcement_type", columnList = "type"),
        @Index(name = "idx_announcement_priority", columnList = "priority"),
        @Index(name = "idx_announcement_publish_date", columnList = "publishDate"),
        @Index(name = "idx_announcement_expiry_date", columnList = "expiryDate"),
        @Index(name = "idx_announcement_target_audience", columnList = "targetAudience")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content cannot exceed 2000 characters")
    @Column(nullable = false, columnDefinition = "TEXT") // TEXT for potentially longer content
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AnnouncementType type = AnnouncementType.GENERAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AnnouncementPriority priority = AnnouncementPriority.MEDIUM;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AnnouncementTargetAudience targetAudience = AnnouncementTargetAudience.ALL;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "announcement_target_room_map",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private Set<Room> targetRooms = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "announcement_target_user_map",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> targetUsers = new HashSet<>();

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    private LocalDateTime publishDate; // Set when status becomes PUBLISHED

    private LocalDateTime expiryDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "announcement_attachments", joinColumns = @JoinColumn(name = "announcement_id"))
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ReadReceipt> readReceipts = new HashSet<>();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Like> likes = new HashSet<>();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("commentedAt ASC")
    private List<Comment> commentEntries = new ArrayList<>(); // Renamed from 'comments'

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "announcement_tags", joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false)
    private boolean isSticky = false;

    @Column(nullable = false)
    private boolean emailSent = false;

    @Column(nullable = false)
    private boolean smsSent = false; // Assuming this might be added later

    @Column(nullable = false)
    private boolean notificationSent = false; // For in-app notifications

    @Column(nullable = false)
    private int viewCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Virtual properties from Mongoose (like readPercentage, likeCount, commentCount, isActive)
    // will be implemented as methods in a service layer or DTOs, not directly in the entity.

    // Mongoose pre-save for expiry: This logic will be in the service layer.
    // Mongoose methods (markAsRead, addLike, removeLike, addComment):
    // These will also be part of the AnnouncementService.
}
