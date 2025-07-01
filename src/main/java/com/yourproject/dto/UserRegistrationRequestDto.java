package com.yourproject.dto;

import com.yourproject.entity.Role;
import com.yourproject.entity.Gender;
import com.yourproject.entity.embeddable.Address;
import com.yourproject.entity.embeddable.EmergencyContact;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequestDto {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    // Consider adding regex for password complexity if needed, e.g. @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,}$")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Please enter a valid 10-digit phone number")
    private String phone;

    @NotNull(message = "Address is required")
    private Address address; // Assuming Address DTO will be used or direct fields

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Emergency contact is required")
    private EmergencyContact emergencyContact; // Assuming EmergencyContact DTO or direct fields

    // Student specific fields (optional, based on role)
    private String course;
    private Integer year;

    // Staff specific fields (optional, based on role)
    private String department;
}
