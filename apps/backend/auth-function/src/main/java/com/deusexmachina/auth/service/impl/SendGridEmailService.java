package com.deusexmachina.auth.service.impl;

import com.deusexmachina.auth.service.EmailService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * SendGrid implementation of EmailService.
 * Follows Single Responsibility Principle - only handles email sending.
 */
@Singleton
public class SendGridEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);
    
    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String fromName;
    private final String baseUrl;
    
    // Template IDs (should be configured in SendGrid)
    private static final String VERIFICATION_TEMPLATE_ID = "d-verification-template";
    private static final String PASSWORD_RESET_TEMPLATE_ID = "d-password-reset-template";
    private static final String NEW_DEVICE_TEMPLATE_ID = "d-new-device-template";
    private static final String MFA_CODE_TEMPLATE_ID = "d-mfa-code-template";
    private static final String ACCOUNT_LOCKED_TEMPLATE_ID = "d-account-locked-template";
    
    @Inject
    public SendGridEmailService(
            @Named("sendgrid.api.key") String apiKey,
            @Named("email.from.address") String fromEmail,
            @Named("email.from.name") String fromName,
            @Named("app.base.url") String baseUrl) {
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.baseUrl = baseUrl;
    }
    
    @Override
    public CompletableFuture<Void> sendVerificationEmail(String email, String verificationToken) {
        return sendTemplateEmail(email, VERIFICATION_TEMPLATE_ID, 
                "email", email,
                "verification_link", baseUrl + "/auth/verify-email?token=" + verificationToken,
                "app_name", "DeusExMachina"
        );
    }
    
    @Override
    public CompletableFuture<Void> sendPasswordResetEmail(String email, String resetToken) {
        return sendTemplateEmail(email, PASSWORD_RESET_TEMPLATE_ID,
                "email", email,
                "reset_link", baseUrl + "/auth/reset-password?token=" + resetToken,
                "app_name", "DeusExMachina",
                "expiry_hours", "1"
        );
    }
    
    @Override
    public CompletableFuture<Void> sendNewDeviceLoginNotification(String email, String deviceInfo, String ipAddress) {
        return sendTemplateEmail(email, NEW_DEVICE_TEMPLATE_ID,
                "email", email,
                "device_info", deviceInfo,
                "ip_address", ipAddress,
                "app_name", "DeusExMachina",
                "security_link", baseUrl + "/account/security"
        );
    }
    
    @Override
    public CompletableFuture<Void> sendMfaCode(String email, String code) {
        return sendTemplateEmail(email, MFA_CODE_TEMPLATE_ID,
                "email", email,
                "mfa_code", code,
                "app_name", "DeusExMachina",
                "expiry_minutes", "5"
        );
    }
    
    @Override
    public CompletableFuture<Void> sendAccountLockedNotification(String email, int attempts) {
        return sendTemplateEmail(email, ACCOUNT_LOCKED_TEMPLATE_ID,
                "email", email,
                "attempts", String.valueOf(attempts),
                "app_name", "DeusExMachina",
                "support_link", baseUrl + "/support",
                "unlock_time", "15 minutes"
        );
    }
    
    private CompletableFuture<Void> sendTemplateEmail(String toEmail, String templateId, 
                                                      String... dynamicData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Mail mail = new Mail();
                mail.setFrom(new Email(fromEmail, fromName));
                mail.setTemplateId(templateId);
                
                Personalization personalization = new Personalization();
                personalization.addTo(new Email(toEmail));
                
                // Add dynamic template data
                for (int i = 0; i < dynamicData.length; i += 2) {
                    personalization.addDynamicTemplateData(dynamicData[i], dynamicData[i + 1]);
                }
                
                mail.addPersonalization(personalization);
                
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                
                Response response = sendGrid.api(request);
                
                if (response.getStatusCode() >= 400) {
                    logger.error("Failed to send email to {}: {} - {}", 
                            toEmail, response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Email sending failed: " + response.getBody());
                }
                
                logger.info("Email sent successfully to {} using template {}", toEmail, templateId);
                return null;
                
            } catch (IOException e) {
                logger.error("Failed to send email to " + toEmail, e);
                throw new RuntimeException("Email sending failed", e);
            }
        });
    }
    
    // Fallback method for sending plain emails without templates
    private CompletableFuture<Void> sendPlainEmail(String toEmail, String subject, String htmlContent) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Email from = new Email(fromEmail, fromName);
                Email to = new Email(toEmail);
                Content content = new Content("text/html", htmlContent);
                Mail mail = new Mail(from, subject, to, content);
                
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                
                Response response = sendGrid.api(request);
                
                if (response.getStatusCode() >= 400) {
                    logger.error("Failed to send email to {}: {} - {}", 
                            toEmail, response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Email sending failed: " + response.getBody());
                }
                
                logger.info("Plain email sent successfully to {}", toEmail);
                return null;
                
            } catch (IOException e) {
                logger.error("Failed to send email to " + toEmail, e);
                throw new RuntimeException("Email sending failed", e);
            }
        });
    }
}