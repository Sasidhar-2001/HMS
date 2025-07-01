package com.yourproject.dto;

import com.yourproject.entity.RoomType;
import com.yourproject.entity.RoomStatus;
import com.yourproject.entity.Amenity;
import com.yourproject.entity.embeddable.MaintenanceRecord; // Assuming a MaintenanceRecordDto might be better
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id;
    private String roomNumber;
    private Integer floor;
    private String block;
    private RoomType type;
    private Integer capacity;
    private int currentOccupancyCount; // Calculated
    // List<UserSlimDto> occupants; // Or List<OccupancyDto>
    private List<OccupancyDto> currentOccupancies; // More detailed than just UserSlimDto
    private Set<Amenity> amenities;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private RoomStatus status;
    private List<MaintenanceRecordDto> maintenanceHistory; // Changed to DTO
    private List<String> images; // URLs
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isAvailable; // Calculated
}
