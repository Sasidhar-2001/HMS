package com.yourproject.service.impl;

import com.yourproject.service.EmailService;
import com.yourproject.entity.User;
// import com.yourproject.entity.Fee;
// import com.yourproject.entity.Announcement;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async; // For sending emails asynchronously
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine; // If using Thymeleaf for templates
import org.thymeleaf.context.Context; // If using Thymeleaf for templates

import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine emailTemplateEngine; // If using Thymeleaf

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine emailTemplateEngine) { // Injected templateEngine
        this.mailSender = mailSender;
        this.emailTemplateEngine = emailTemplateEngine; // Use the "emailTemplateEngine" bean
    }

    @Override
    @Async // Good practice to send emails asynchronously
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            // Log error, handle exception (e.g., queue for retry)
            System.err.println("Error sending simple email: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            System.err.println("Error sending HTML email: " + e.getMessage());
        }
    }

    // Placeholder implementations for specific emails based on Node.js version
    // These would use a templating engine in a real application.

    // Example using Thymeleaf for Welcome Email
    // This method should be uncommented and other similar methods updated in EmailService interface and here.
    /*
    @Override
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to Hostel Management System!";
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("subject", subject); // Make subject available in template if needed
        // Assuming template is at src/main/resources/templates/email/welcome-email.html
        String htmlContent = emailTemplateEngine.process("welcome-email", context);
        sendHtmlMessage(user.getEmail(), subject, htmlContent);
    }
    */

    // @Override
    // public void sendPasswordResetEmail(User user, String resetUrl) {
    //     String subject = "Password Reset Request";
    //     String htmlContent = String.format("<p>Dear %s,</p><p>Click <a href=\"%s\">here</a> to reset your password.</p>",
    //                                      user.getFirstName(), resetUrl);
    //     sendHtmlMessage(user.getEmail(), subject, htmlContent);
    // }

    // @Override
    // public void sendFeeReminderEmail(User student, Fee fee) {
    //     String subject = String.format("Fee Reminder: %s due on %s", fee.getFeeType(), fee.getDueDate());
    //     String htmlContent = String.format("<p>Dear %s,</p><p>Reminder for fee: %s, Amount: %.2f, Due: %s</p>",
    //                                     student.getFirstName(), fee.getFeeType(), fee.getFinalAmount(), fee.getDueDate());
    //     sendHtmlMessage(student.getEmail(), subject, htmlContent);
    // }

    // @Override
    // public void sendAnnouncementEmail(List<User> recipients, Announcement announcement) {
    //     String subject = announcement.getTitle();
    //     // Simplified content, ideally use a template
    //     String htmlContent = String.format("<h3>%s</h3><p>%s</p>", announcement.getTitle(), announcement.getContent());
    //     for (User recipient : recipients) {
    //         sendHtmlMessage(recipient.getEmail(), subject, htmlContent);
    //     }
    // }

    // @Override
    // public void sendBulkEmail(List<String> recipients, String subject, String templateName, Map<String, Object> templateModel) {
    //     // Context context = new Context();
    //     // context.setVariables(templateModel);
    //     // String htmlContent = templateEngine.process(templateName, context);
    //     String htmlContent = "Bulk email content for: " + subject; // Simplified
    //     for (String recipient : recipients) {
    //         sendHtmlMessage(recipient, subject, htmlContent);
    //     }
    // }

}
