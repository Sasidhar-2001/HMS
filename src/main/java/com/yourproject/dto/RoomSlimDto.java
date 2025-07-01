package com.yourproject.dto;

import com.yourproject.entity.RoomType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomSlimDto {
    private Long id;
    private String roomNumber;
    private String block;
    private Integer floor;
    private RoomType type;
    // Add any other essential 'slim' details needed when Room is nested.
}
