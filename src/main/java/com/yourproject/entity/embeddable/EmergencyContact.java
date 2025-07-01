package com.yourproject.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @NotBlank(message = "Emergency contact name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Emergency contact phone is required")
    @Size(max = 20)
    // Consider adding @Pattern for phone number format if needed
    private String phone;

    @NotBlank(message = "Emergency contact relation is required")
    @Size(max = 50)
    private String relation;
}
