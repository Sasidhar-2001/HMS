package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeStatsDto {
    private int year;
    private long totalFeeEntries;
    private long paidFeeEntries;
    private long pendingFeeEntries;
    private long overdueFeeEntries;
    private BigDecimal totalRevenueCollected; // Sum of finalAmount for 'paid' fees
    private BigDecimal totalPendingRevenue;   // Sum of balanceAmount for 'pending', 'overdue', 'partial'
    private Double collectionRatePercentage;  // (paidFeeEntries / totalFeeEntries) * 100 or based on amount
    private List<Map<String, Object>> monthlyStats; // e.g., [{month: 1, totalAmount: X, paidAmount: Y}]
    private List<Map<String, Object>> feeTypeStats; // e.g., [{feeType: 'ROOM_RENT', totalAmount: X, paidAmount: Y}]
}
