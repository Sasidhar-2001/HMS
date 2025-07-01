package com.yourproject.dto;

import com.yourproject.entity.ComplaintStatus;
import com.yourproject.entity.embeddable.ResolutionDetails; // For including resolution details during status update
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusUpdateRequestDto {

    @NotNull(message = "New status is required")
    private ComplaintStatus status;

    @Size(max = 500)
    private String comment; // For status change reason or resolution notes

    private Long assignedToId; // Optional: To assign/reassign during status update

    // Optional: If status is RESOLVED or CLOSED, resolution details can be provided.
    // This assumes ResolutionDetails is suitable as a DTO or part of this request.
    private ResolutionDetails resolution;
}
