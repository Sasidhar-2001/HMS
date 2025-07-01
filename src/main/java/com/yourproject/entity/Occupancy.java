package com.yourproject.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "occupancies", indexes = {
        @Index(name = "idx_occupancy_user", columnList = "user_id"),
        @Index(name = "idx_occupancy_room", columnList = "room_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Occupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate allocatedDate;

    private LocalDate vacatedDate; // To mark when the student vacates

    private Integer bedNumber;

    @Column(nullable = false)
    private boolean isActive = true; // True if this is the current occupancy for the student in this room

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // This entity represents a student's stay in a room.
    // A User might have multiple past Occupancy records but only one active.
    // A Room will have multiple Occupancy records up to its capacity.

    // We might need constraints to ensure a student has only one active occupancy,
    // and a room does not exceed its capacity with active occupancies.
    // These are typically handled at the service layer or with database triggers/constraints.
}
