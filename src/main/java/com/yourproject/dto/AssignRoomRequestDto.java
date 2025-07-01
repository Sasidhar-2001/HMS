package com.yourproject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoomRequestDto {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Room ID is required")
    private Long roomId; // This is for adminController.assignRoom. RoomController.assignStudent uses room ID from path.

    private Integer bedNumber; // Optional, can be auto-assigned
}
