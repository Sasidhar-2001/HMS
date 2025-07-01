package com.yourproject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {

    @NotBlank(message = "Report type is required")
    // Example types, can be an enum later
    @Pattern(regexp = "students|fees|complaints|rooms|leaves", message = "Invalid report type")
    private String type;

    @NotBlank(message = "Report format is required")
    @Pattern(regexp = "pdf|excel", message = "Invalid report format. Must be 'pdf' or 'excel'.")
    private String format;

    private LocalDate startDate; // Optional
    private LocalDate endDate;   // Optional
}
