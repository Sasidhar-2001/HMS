package com.yourproject.entity;

public enum AnnouncementTargetAudience {
    ALL,
    STUDENTS,
    WARDENS,
    ADMINS,
    SPECIFIC_ROOMS, // This implies a ManyToMany with Room
    SPECIFIC_USERS  // This implies a ManyToMany with User
}
