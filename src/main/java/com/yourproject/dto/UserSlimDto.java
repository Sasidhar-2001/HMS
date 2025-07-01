package com.yourproject.dto;

import com.yourproject.entity.Role;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSlimDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private Role role;
    private String studentId; // if student
    private String employeeId; // if staff
    private String profileImageUrl;

    public UserSlimDto(Long id, String firstName, String lastName, String email, Role role, String studentId, String employeeId, String profileImageUrl) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = firstName + " " + lastName;
        this.email = email;
        this.role = role;
        this.studentId = studentId;
        this.employeeId = employeeId;
        this.profileImageUrl = profileImageUrl;
    }
}
