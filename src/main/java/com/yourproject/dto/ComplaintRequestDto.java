package com.yourproject.dto;

import com.yourproject.entity.ComplaintCategory;
import com.yourproject.entity.ComplaintPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequestDto {

    @NotBlank(message = "Complaint title is required")
    @Size(max = 100)
    private String title;

    @NotBlank(message = "Complaint description is required")
    @Size(max = 1000)
    private String description;

    @NotNull(message = "Complaint category is required")
    private ComplaintCategory category;

    private ComplaintPriority priority = ComplaintPriority.MEDIUM; // Default

    // Status is usually set by system or admin, not in initial creation by student.
    // private ComplaintStatus status;

    // reportedBy is set from authenticated user.

    private Long assignedToId; // Optional: For admin/warden creating and assigning
    private Long roomId;       // Optional: If complaint is tied to a specific room

    @Size(max = 255)
    private String location;

    // For image uploads, this might be handled by multipart request.
    // This DTO field could be for providing URLs of already uploaded images if that's the flow.
    private List<String> images;

    private LocalDate expectedResolutionDate; // Optional

    private Set<String> tags;
}
