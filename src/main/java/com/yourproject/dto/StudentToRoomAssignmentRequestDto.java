package com.yourproject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentToRoomAssignmentRequestDto {
    @NotNull(message = "Student ID is required")
    private Long studentId;
    private Integer bedNumber; // Optional
}
