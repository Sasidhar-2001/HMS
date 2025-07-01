package com.yourproject.entity.embeddable;

import com.yourproject.entity.ReminderType;
import com.yourproject.entity.ReminderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime; // Mongoose schema used Date

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeReminder {

    @NotNull
    @Column(nullable = false)
    private LocalDateTime sentDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReminderType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReminderStatus status;
}
