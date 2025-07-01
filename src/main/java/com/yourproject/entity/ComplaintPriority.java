package com.yourproject.entity;

// Renaming to avoid conflict if there's a generic Priority enum later.
// Or could use a shared Priority enum if the values are identical.
// For now, specific to Complaint.
public enum ComplaintPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
