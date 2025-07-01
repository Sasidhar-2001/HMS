package com.yourproject.dto;

import com.yourproject.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeePaymentRequestDto {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than 0")
    private BigDecimal amount;

    private LocalDate paidDate; // Defaults to now if not provided

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 100)
    private String transactionId;

    @Size(max = 100)
    private String receiptNumber; // Optional, can be auto-generated
}
