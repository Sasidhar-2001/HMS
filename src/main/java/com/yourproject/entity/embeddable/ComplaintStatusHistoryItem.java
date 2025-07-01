package com.yourproject.entity.embeddable;

import com.yourproject.entity.ComplaintStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintStatusHistoryItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ComplaintStatus status;

    // Storing user ID is simpler for an embeddable.
    // If full User object is needed, this history should be a separate @OneToMany entity.
    private Long updatedById;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Size(max = 500)
    @Column(length = 500)
    private String comment;
}
