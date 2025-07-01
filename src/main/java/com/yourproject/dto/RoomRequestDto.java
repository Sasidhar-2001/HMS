package com.yourproject.dto;

import com.yourproject.entity.RoomType;
import com.yourproject.entity.RoomStatus;
import com.yourproject.entity.Amenity;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequestDto {

    @NotBlank(message = "Room number is required")
    @Size(max = 20)
    private String roomNumber;

    @NotNull(message = "Floor number is required")
    @Min(value = 0, message = "Floor cannot be negative")
    private Integer floor;

    @NotBlank(message = "Block is required")
    @Size(max = 20)
    private String block;

    @NotNull(message = "Room type is required")
    private RoomType type;

    @NotNull(message = "Room capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private Set<Amenity> amenities;

    @NotNull(message = "Monthly rent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rent cannot be negative")
    private BigDecimal monthlyRent;

    @NotNull(message = "Security deposit is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Security deposit cannot be negative")
    private BigDecimal securityDeposit;

    private RoomStatus status; // Optional on creation, defaults to AVAILABLE

    private List<String> images; // List of image URLs/paths

    @Size(max = 2000)
    private String description;

    private Boolean isActive; // For updates mainly
}
