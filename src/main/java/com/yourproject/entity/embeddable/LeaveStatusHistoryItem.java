package com.yourproject.entity.embeddable;

import com.yourproject.entity.LeaveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatusHistoryItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private LeaveStatus status;

    // ID of the user (student, admin, or warden) who updated the status
    private Long updatedById;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Size(max = 500)
    @Column(length = 500)
    private String comment; // e.g., rejection reason
}
