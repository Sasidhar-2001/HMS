package com.yourproject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentIdRequestDto {
    @NotNull(message = "Student ID is required")
    private Long studentId;
}
