package com.yourproject.dto;

import com.yourproject.entity.LeaveType;
import com.yourproject.entity.embeddable.EmergencyContact;
import com.yourproject.entity.embeddable.LeaveDestination;
import com.yourproject.entity.embeddable.ParentalApproval;
import com.yourproject.entity.embeddable.MedicalCertificateInfo;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDto {

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date must be today or in the future")
    private LocalDate endDate;

    @NotBlank(message = "Reason is required")
    @Size(max = 500)
    private String reason;

    // Student ID is set from authenticated user in service.

    private EmergencyContact emergencyContact; // Optional, but good practice

    private LeaveDestination destination; // Optional

    // For attachments, similar to other entities, client might send metadata or new files.
    // This field might be for URLs if files are pre-uploaded.
    private List<String> attachments;

    // These are usually determined by policy or leave type, set by service.
    // However, a student might indicate they have obtained/uploaded them.
    private ParentalApproval parentalApprovalInfo; // Optional from student's side
    private MedicalCertificateInfo medicalCertificateInfo; // Optional from student's side
}
