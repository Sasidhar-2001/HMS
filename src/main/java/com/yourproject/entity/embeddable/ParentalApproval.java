package com.yourproject.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate; // Mongoose used Date

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentalApproval {

    @Column(nullable = false)
    private boolean isRequired = false;

    @Column(nullable = false)
    private boolean isObtained = false;

    @Size(max = 20)
    @Column(length = 20)
    private String contactNumber; // Parent's contact number

    private LocalDate approvalDate;
}
