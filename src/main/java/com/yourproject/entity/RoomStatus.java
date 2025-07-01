package com.yourproject.entity;

public enum RoomStatus {
    AVAILABLE,    // Room is available for allocation
    OCCUPIED,     // Room is fully or partially occupied (depending on exact logic)
    MAINTENANCE,  // Room is under maintenance
    RESERVED      // Room is reserved but not yet occupied
}
