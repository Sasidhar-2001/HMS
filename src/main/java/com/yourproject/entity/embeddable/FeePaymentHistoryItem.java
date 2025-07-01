package com.yourproject.entity.embeddable;

import com.yourproject.entity.PaymentMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate; // Mongoose schema used Date

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentHistoryItem {

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(nullable = false)
    private LocalDate paidDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Size(max = 100)
    @Column(length = 100)
    private String transactionId;

    @Size(max = 100)
    @Column(length = 100)
    private String receiptNumber;

    // Storing user ID who made the payment.
    // Could be the student themselves or an admin/warden processing it.
    private Long paidById;
}
