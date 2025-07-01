package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveStatsDto {
    private int year;
    private long totalLeaves;
    private long pendingLeaves;
    private long approvedLeaves;
    private long rejectedLeaves;
    private long activeLeaves; // Currently on leave
    private long overdueLeaves; // Approved, end date passed, not returned
    private List<Map<String, Object>> leavesByType; // e.g., [{type: 'MEDICAL', count: 5}]
    private List<Map<String, Object>> monthlyStats; // e.g., [{month: 1, count: 10, avgDuration: 3.5}]
}
