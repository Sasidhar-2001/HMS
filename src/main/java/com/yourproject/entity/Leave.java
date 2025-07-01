package com.yourproject.entity;

import com.yourproject.entity.embeddable.*;
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
import java.util.List;

@Entity
@Table(name = "leaves", indexes = {
        @Index(name = "idx_leave_leave_id_str", columnList = "leaveIdString", unique = true),
        @Index(name = "idx_leave_student", columnList = "student_id"),
        @Index(name = "idx_leave_status", columnList = "status"),
        @Index(name = "idx_leave_start_date", columnList = "startDate"),
        @Index(name = "idx_leave_end_date", columnList = "endDate"),
        @Index(name = "idx_leave_type", columnList = "leaveType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Leave ID string is required for tracking")
    @Column(name = "leaveIdString", unique = true, nullable = false, length = 50)
    private String leaveIdString; // Human-readable ID like LV...

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotNull(message = "Leave type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(nullable = false)
    private LocalDate endDate;

    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Column(nullable = false, length = 500)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy; // Admin or Warden who approved

    private LocalDateTime approvedDate;

    @Size(max = 500)
    @Column(length = 500)
    private String rejectionReason;

    // Reusing the common EmergencyContact embeddable
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "emergency_contact_name", length = 100)),
            @AttributeOverride(name = "phone", column = @Column(name = "emergency_contact_phone", length = 20)),
            @AttributeOverride(name = "relation", column = @Column(name = "emergency_contact_relation", length = 50))
    })
    private EmergencyContact emergencyContact;

    @Embedded
    private LeaveDestination destination;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "leave_attachments", joinColumns = @JoinColumn(name = "leave_id"))
    @Column(name = "attachment_url", length = 255) // Storing URLs or paths to attachments
    private List<String> attachments = new ArrayList<>();

    private LocalDate actualReturnDate;

    @Column(nullable = false)
    private boolean isExtended = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "leave_extension_requests", joinColumns = @JoinColumn(name = "leave_id"))
    @OrderBy("requestedDate DESC")
    private List<LeaveExtensionRequest> extensionRequests = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "leave_status_history", joinColumns = @JoinColumn(name = "leave_id"))
    @OrderBy("updatedAt DESC")
    private List<LeaveStatusHistoryItem> statusHistory = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "isRequired", column = @Column(name = "parent_approval_required")),
            @AttributeOverride(name = "isObtained", column = @Column(name = "parent_approval_obtained")),
            @AttributeOverride(name = "contactNumber", column = @Column(name = "parent_contact_number", length = 20)),
            @AttributeOverride(name = "approvalDate", column = @Column(name = "parent_approval_date"))
    })
    private ParentalApproval parentalApprovalInfo; // Renamed to avoid conflict

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "isRequired", column = @Column(name = "medical_cert_required")),
            @AttributeOverride(name = "isUploaded", column = @Column(name = "medical_cert_uploaded")),
            @AttributeOverride(name = "fileName", column = @Column(name = "medical_cert_filename", length = 255)),
            @AttributeOverride(name = "uploadDate", column = @Column(name = "medical_cert_upload_date"))
    })
    private MedicalCertificateInfo medicalCertificateInfo;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Mongoose pre-save logic (leaveIdString generation, medical cert requirement, parent approval requirement):
    // This will be handled in the LeaveService.

    // Mongoose date validation (endDate > startDate):
    // This will be handled by custom validation annotation or in the service layer.

    // Mongoose methods (updateStatus, requestExtension):
    // These will be part of LeaveService.

    // Virtuals (durationDays, currentStatus, overdueDays):
    // These will be calculated in DTOs or service layer.
}
