package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRecordDto {
    private String issue;
    private LocalDate reportedDate;
    private LocalDate resolvedDate;
    private BigDecimal cost;
    private String description;
    private Long reportedById; // Or UserSlimDto reportedBy;
    private String reportedByName; // If only name is needed
}
