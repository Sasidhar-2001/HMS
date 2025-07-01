package com.yourproject.dto;

import com.yourproject.entity.Role;
import com.yourproject.entity.Gender;
import com.yourproject.entity.embeddable.Address;
import com.yourproject.entity.embeddable.EmergencyContact;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName; // Convenience
    private String email;
    private Role role;
    private String phone;
    private Address address;
    private LocalDate dateOfBirth;
    private Gender gender;
    private EmergencyContact emergencyContact;

    // Student specific
    private String studentId;
    private String course;
    private Integer year;
    private RoomSlimDto currentRoom; // Simplified Room DTO

    // Staff specific
    private String employeeId;
    private String department;
    private LocalDate joinDate;

    private boolean isActive;
    private String profileImageUrl;
    private LocalDateTime lastLogin;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // We might not want to expose all occupancies directly in UserDto,
    // but currentRoom is useful.
    // List<OccupancyDto> occupancies; // If needed
}
