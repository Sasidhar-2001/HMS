package com.yourproject.dto;

import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkReturnRequestDto {

    // Optional: if not provided, service will assume current date
    @PastOrPresent(message = "Actual return date must be in the past or present")
    private LocalDate actualReturnDate;
}
