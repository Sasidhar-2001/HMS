package com.yourproject.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyDto {
    private Long id;
    private UserSlimDto student; // To avoid circular UserDto -> OccupancyDto -> UserDto
    // private RoomSlimDto room; // Usually not needed if OccupancyDto is part of RoomDto
    private LocalDate allocatedDate;
    private LocalDate vacatedDate;
    private Integer bedNumber;
    private boolean isActive;
    private LocalDateTime createdAt;
}
