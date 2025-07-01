package com.yourproject.dto;

import com.yourproject.entity.LeaveStatus;
import com.yourproject.entity.LeaveType;
import com.yourproject.entity.embeddable.*; // Assuming direct use of embeddables for response
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDto {
    private Long id;
    private String leaveIdString; // Human-readable ID
    private UserSlimDto student;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus status;
    private LocalDateTime appliedDate;
    private UserSlimDto approvedBy;
    private LocalDateTime approvedDate;
    private String rejectionReason;
    private EmergencyContact emergencyContact; // Reusing common embeddable
    private LeaveDestination destination;
    private List<String> attachments; // URLs
    private LocalDate actualReturnDate;
    private boolean isExtended;
    private List<LeaveExtensionRequest> extensionRequests; // Using embeddable directly
    private List<LeaveStatusHistoryItem> statusHistory;   // Using embeddable directly
    private ParentalApproval parentalApprovalInfo;
    private MedicalCertificateInfo medicalCertificateInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields
    private Integer durationDays;
    private String currentOverallStatus; // e.g., "Upcoming", "Active", "Overdue", "Completed"
    private Integer overdueDays; // If applicable
}
