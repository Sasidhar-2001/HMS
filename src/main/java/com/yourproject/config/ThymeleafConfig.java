package com.yourproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;

@Configuration
public class ThymeleafConfig {

    // Configure Template Resolver for HTML emails
    @Bean
    public ITemplateResolver htmlEmailTemplateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/email/"); // Location of email templates
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateResolver.setCacheable(false); // Set to true in production for performance
        templateResolver.setOrder(1); // Order if multiple resolvers
        return templateResolver;
    }

    // Configure Template Engine
    // Note: Spring Boot auto-configures a SpringTemplateEngine if thymeleaf starter is present.
    // This bean definition is for explicit configuration or if specific email template resolver is needed.
    // If Spring Boot's auto-configured one is sufficient and can find the email templates,
    // this explicit SpringTemplateEngine bean might not be strictly necessary,
    // but it allows fine-tuning for email-specific templates.
    @Bean(name = "emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine(ITemplateResolver htmlEmailTemplateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(htmlEmailTemplateResolver);
        // Add dialects if needed, e.g., LayoutDialect, SpringSecurityDialect (less common for emails)
        return templateEngine;
    }
}
