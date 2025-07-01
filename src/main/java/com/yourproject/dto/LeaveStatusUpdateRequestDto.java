package com.yourproject.dto;

import com.yourproject.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatusUpdateRequestDto {

    @NotNull(message = "New status is required")
    private LeaveStatus status; // APPROVED, REJECTED, CANCELLED

    @Size(max = 500)
    private String comment; // e.g., Rejection reason or cancellation reason
}
