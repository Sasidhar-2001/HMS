package com.yourproject.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveExtensionApiRequestDto {

    @NotNull(message = "New end date for extension is required")
    @Future(message = "New end date must be in the future")
    private LocalDate newEndDate;

    @NotBlank(message = "Reason for extension is required")
    @Size(max = 500)
    private String reason;
}
