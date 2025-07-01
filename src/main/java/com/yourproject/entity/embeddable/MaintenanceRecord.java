package com.yourproject.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecord {

    @Size(max = 255)
    @Column(length = 255)
    private String issue;

    private LocalDate reportedDate;

    private LocalDate resolvedDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(columnDefinition = "TEXT")
    private String description;

    // In the JS model, reportedBy was a ref to User.
    // For an Embeddable, storing the user ID might be simpler,
    // or this needs to be a separate @OneToMany entity relationship if full User object is needed.
    // For simplicity with Embeddable, let's store reportedBy user's ID or name.
    // Storing ID is better for potential future linking.
    private Long reportedById; // Or String reportedByName;

    // Alternatively, if reportedBy is not critical or always system/admin:
    // private String reportedBySource; (e.g. "Admin Action", "System")
}
