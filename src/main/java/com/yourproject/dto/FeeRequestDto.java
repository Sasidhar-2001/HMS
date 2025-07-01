package com.yourproject.dto;

import com.yourproject.entity.FeeType;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeRequestDto {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Month is required")
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(2000) // Or a relevant minimum year
    private Integer year;

    @Size(max = 500)
    private String description;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal lateFee; // Optional, defaults to 0

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal discount; // Optional, defaults to 0

    private Long roomId; // Optional, if fee is specific to a room

    @Size(max = 1000)
    private String notes;
}
