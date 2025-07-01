package com.yourproject.entity.embeddable;

import com.yourproject.entity.LeaveExtensionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate; // Mongoose used Date
import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveExtensionRequest {

    @NotNull
    private LocalDate requestedEndDate;

    @Size(max = 500)
    @Column(length = 500)
    private String reason;

    @NotNull
    private LocalDateTime requestedDate = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LeaveExtensionStatus status = LeaveExtensionStatus.PENDING;

    // ID of the admin/warden who approved/rejected
    private Long approvedById;

    private LocalDateTime approvedDate;
}
