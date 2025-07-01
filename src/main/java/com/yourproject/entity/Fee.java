package com.yourproject.entity;

import com.yourproject.entity.embeddable.FeePaymentHistoryItem;
import com.yourproject.entity.embeddable.FeeReminder;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fees", indexes = {
        @Index(name = "idx_fee_student", columnList = "student_id"),
        @Index(name = "idx_fee_status", columnList = "status"),
        @Index(name = "idx_fee_due_date", columnList = "dueDate"),
        @Index(name = "idx_fee_month_year", columnList = "month, year"),
        @Index(name = "idx_fee_type", columnList = "feeType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotNull(message = "Fee type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeeType feeType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0") // Original had min:0, but usually fee > 0
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate paidDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeeStatus status = FeeStatus.PENDING;

    // paymentMethod and transactionId are for the latest payment, history is in paymentHistory
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentMethod paymentMethod;

    @Size(max = 100)
    @Column(length = 100)
    private String transactionId;

    @Size(max = 100)
    @Column(length = 100)
    private String receiptNumber; // For the final payment or a consolidated one

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Column(nullable = false)
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be valid") // Adjust min year as appropriate
    @Column(nullable = false)
    private Integer year;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Late fee cannot be negative")
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount cannot be negative")
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull(message = "Final amount is required") // Calculated: amount + lateFee - discount
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true, message = "Paid amount cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // balanceAmount is calculated: finalAmount - paidAmount
    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAmount;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "fee_payment_history", joinColumns = @JoinColumn(name = "fee_id"))
    @OrderBy("paidDate ASC")
    private List<FeePaymentHistoryItem> paymentHistory = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "fee_reminders", joinColumns = @JoinColumn(name = "fee_id"))
    @OrderBy("sentDate DESC")
    private List<FeeReminder> reminders = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id") // If fee is specific to a room (e.g. room_rent for a past room)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;

    @Size(max = 1000)
    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Mongoose pre-save logic (calculating finalAmount, balanceAmount, updating status, generating receiptNumber):
    // This will be handled in the FeeService before saving.

    // Mongoose methods (addPayment, addReminder):
    // These will be part of FeeService.

    // Virtuals (daysOverdue, paymentPercentage):
    // These will be calculated in DTOs or service layer.
}
