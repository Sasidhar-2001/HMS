package com.yourproject.entity;

import com.yourproject.entity.embeddable.Address;
import com.yourproject.entity.embeddable.EmergencyContact;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
// Required for JPA 2.2 / Hibernate 5.2+ for Auditing with @EntityListeners
// import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_student_id", columnList = "studentId", unique = true),
        @Index(name = "idx_user_employee_id", columnList = "employeeId", unique = true),
        @Index(name = "idx_user_role", columnList = "role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
// @EntityListeners(AuditingEntityListener.class) // Enable for @CreatedBy, @LastModifiedBy
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Column(nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    // Password length validation handled by DTO/service layer before hashing
    @Column(nullable = false, length = 100) // Store hashed password
    private String password;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Please enter a valid 10-digit phone number")
    @Column(nullable = false, length = 20)
    private String phone;

    @Embedded
    private Address address;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @NotNull(message = "Emergency contact is required")
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "emergency_contact_name", length = 100)),
            @AttributeOverride(name = "phone", column = @Column(name = "emergency_contact_phone", length = 20)),
            @AttributeOverride(name = "relation", column = @Column(name = "emergency_contact_relation", length = 50))
    })
    private EmergencyContact emergencyContact;

    // Student specific fields
    @Column(unique = true, length = 50)
    private String studentId; // To be generated

    @Size(max = 100)
    @Column(length = 100)
    private String course;

    @Min(value = 1, message = "Year must be at least 1")
    @Max(value = 10, message = "Year can be at most 10") // Adjust as needed
    private Integer year;

    // A user can have many occupancy records over time (e.g., moving rooms)
    // The 'student' field in Occupancy entity is the owning side.
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Occupancy> occupancies = new ArrayList<>();

    // Staff specific fields (Warden, Admin)
    @Column(unique = true, length = 50)
    private String employeeId; // To be generated

    @Size(max = 100)
    @Column(length = 100)
    private String department;

    private LocalDate joinDate;

    // Common fields
    @Column(nullable = false)
    private boolean isActive = true;

    @Column(length = 255)
    private String profileImageUrl;

    private LocalDateTime lastLogin;

    @Column(nullable = false)
    private boolean emailVerified = false;

    // For password reset functionality
    @Column(length = 255)
    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiry;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // @CreatedBy
    // @Column(updatable = false)
    // private String createdBy;

    // @LastModifiedBy
    // private String lastModifiedBy;

    // Convenience method for full name (not persisted)
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Convenience method to get current (active) room
    @Transient
    public Optional<Room> getCurrentRoom() {
        return occupancies.stream()
                .filter(Occupancy::isActive)
                .findFirst()
                .map(Occupancy::getRoom);
    }

    // Convenience method to get current (active) occupancy details
    @Transient
    public Optional<Occupancy> getCurrentOccupancy() {
        return occupancies.stream()
                .filter(Occupancy::isActive)
                .findFirst();
    }


    // Lifecycle Callbacks for ID generation (similar to Mongoose pre-save)
    // This logic will be better placed in a service layer before persisting.
    // For now, commenting out as it might require access to a sequence or other users.
    /*
    @PrePersist
    public void generateId() {
        if (this.role == Role.STUDENT && this.studentId == null) {
            // Simplified ID generation - replace with robust logic
            this.studentId = "STU" + System.currentTimeMillis() % 10000;
        } else if ((this.role == Role.ADMIN || this.role == Role.WARDEN) && this.employeeId == null) {
            this.employeeId = "EMP" + System.currentTimeMillis() % 10000;
        }
    }
    */

    // Note: Password comparison logic (comparePassword) will be in the service layer.
    // JSON serialization of password will be handled by DTOs.
}
