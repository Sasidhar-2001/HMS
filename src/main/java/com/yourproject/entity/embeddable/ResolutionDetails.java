package com.yourproject.entity.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate; // Mongoose schema used Date, implies just date part often

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionDetails {

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    // Storing user ID is simpler for an embeddable.
    private Long resolvedById;

    private LocalDate resolvedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal cost;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 500)
    @Column(length = 500)
    private String feedback;
}
