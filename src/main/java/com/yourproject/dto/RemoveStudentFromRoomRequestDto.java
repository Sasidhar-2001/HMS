package com.yourproject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveStudentFromRoomRequestDto {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Room ID is required")
    private Long roomId; // Room from which the student is to be removed.
                        // This helps ensure the correct occupancy record is targeted if a student somehow has multiple.
}
