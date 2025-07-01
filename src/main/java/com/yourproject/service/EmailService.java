package com.yourproject.service;

import com.yourproject.entity.User;
// Add other entities if specific email methods are needed for them
// e.g., import com.yourproject.entity.Fee;
// import com.yourproject.entity.Announcement;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendSimpleMessage(String to, String subject, String text);

    void sendHtmlMessage(String to, String subject, String htmlContent);

    // void sendWelcomeEmail(User user);

    // void sendPasswordResetEmail(User user, String resetUrl);

    // void sendFeeReminderEmail(User student, Fee fee);

    // void sendAnnouncementEmail(List<User> recipients, Announcement announcement);

    // void sendBulkEmail(List<String> recipients, String subject, String templateName, Map<String, Object> templateModel);
}
