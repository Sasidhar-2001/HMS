package com.yourproject.dto;

import com.yourproject.entity.FeeStatus;
import com.yourproject.entity.FeeType;
import com.yourproject.entity.PaymentMethod;
import com.yourproject.entity.embeddable.FeePaymentHistoryItem;
import com.yourproject.entity.embeddable.FeeReminder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeDto {
    private Long id;
    private UserSlimDto student;
    private FeeType feeType;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private FeeStatus status;
    private PaymentMethod paymentMethod; // Latest payment method
    private String transactionId;    // Latest transaction ID
    private String receiptNumber;    // Overall receipt number
    private Integer month;
    private Integer year;
    private String description;
    private BigDecimal lateFee;
    private BigDecimal discount;
    private BigDecimal finalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private List<FeePaymentHistoryItem> paymentHistory; // Using embeddable directly
    private List<FeeReminder> reminders;             // Using embeddable directly
    private RoomSlimDto room; // If fee is tied to a room
    private UserSlimDto createdBy;
    private UserSlimDto updatedBy;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields
    private Integer daysOverdue;
    private Integer paymentPercentage;
}
