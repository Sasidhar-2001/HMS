package com.yourproject.entity;

public enum ComplaintStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,     // Different from resolved, might mean verified and no further action.
    REJECTED
}
