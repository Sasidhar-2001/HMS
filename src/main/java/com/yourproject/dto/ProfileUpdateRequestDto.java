package com.yourproject.dto;

import com.yourproject.entity.embeddable.Address;
import com.yourproject.entity.embeddable.EmergencyContact;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Fields here should match what's allowed in authController.updateProfile and studentController.updateProfile
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequestDto {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName; // Optional: only if they can change it

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName; // Optional: only if they can change it

    @Pattern(regexp = "^[0-9]{10}$", message = "Please enter a valid 10-digit phone number")
    private String phone;

    private Address address;

    private EmergencyContact emergencyContact;

    // Student specific, optional
    @Size(max = 100)
    private String course;
    private Integer year;

    // Staff specific, optional
    @Size(max = 100)
    private String department;

    // Profile image URL might be updated via a separate endpoint or included here
    @Size(max = 255)
    private String profileImageUrl;
}
