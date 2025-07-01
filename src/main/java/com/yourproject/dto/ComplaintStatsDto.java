package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatsDto {
    private long totalComplaints;
    private long pendingComplaints;
    private long inProgressComplaints;
    private long resolvedComplaints;
    private List<Map<String, Object>> complaintsByCategory; // e.g., [{category: 'PLUMBING', count: 10}]
    private List<Map<String, Object>> complaintsByPriority; // e.g., [{priority: 'HIGH', count: 5}]
    private Double averageResolutionTimeHours; // Optional
}
