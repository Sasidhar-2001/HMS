package com.yourproject.dto;

import com.yourproject.entity.LeaveExtensionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveExtensionStatusUpdateRequestDto {

    @NotNull(message = "Extension status is required")
    private LeaveExtensionStatus status; // APPROVED, REJECTED

    @Size(max = 500)
    private String comment; // Optional comment, e.g., reason for rejection
}
