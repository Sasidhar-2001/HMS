package com.yourproject.entity;

import com.yourproject.entity.embeddable.MaintenanceRecord;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_room_number", columnList = "roomNumber", unique = true),
        @Index(name = "idx_room_block_floor", columnList = "block, floor"),
        @Index(name = "idx_room_type", columnList = "type"),
        @Index(name = "idx_room_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Room number is required")
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String roomNumber;

    @NotNull(message = "Floor number is required")
    @Min(value = 0, message = "Floor cannot be negative")
    @Column(nullable = false)
    private Integer floor;

    @NotBlank(message = "Block is required")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String block;

    @NotNull(message = "Room type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @NotNull(message = "Room capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Column(nullable = false)
    private Integer capacity;

    // currentOccupancy is derived from active occupancies.
    // This can be calculated in service layer or using a formula if DB supports.
    // @Formula("(SELECT COUNT(o.id) FROM occupancies o WHERE o.room_id = id AND o.is_active = true)")
    // private int currentOccupancy;
    // For now, we will manage this programmatically or assume it's fetched/calculated.

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Occupancy> occupancies = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "amenity", length = 50)
    private Set<Amenity> amenities = new HashSet<>();

    @NotNull(message = "Monthly rent is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rent cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @NotNull(message = "Security deposit is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Security deposit cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDeposit;

    @NotNull(message = "Room status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_maintenance_history", joinColumns = @JoinColumn(name = "room_id"))
    @OrderBy("reportedDate DESC") // Optional: order the history
    private List<MaintenanceRecord> maintenanceHistory = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image_url", length = 255)
    private List<String> images = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // Convenience methods
    @Transient
    public int getCurrentOccupancyCount() {
        return (int) occupancies.stream().filter(Occupancy::isActive).count();
    }

    @Transient
    public boolean isAvailable() {
        return this.status == RoomStatus.AVAILABLE && getCurrentOccupancyCount() < this.capacity;
    }

    // Add occupant logic will be in RoomService, involving creating an Occupancy record.
    // Remove occupant logic will be in RoomService, involving deactivating/removing an Occupancy record.

    // Pre-save logic from Mongoose (updating status based on occupancy)
    // will be handled in the service layer before saving the Room entity.
}
