package com.deusexmachina.email.service;

import com.deusexmachina.auth.service.EmailService;
import com.deusexmachina.email.model.EmailMessage;
import com.deusexmachina.email.model.EmailType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Cloud Pub/Sub implementation of EmailService.
 * Publishes email events to Pub/Sub for asynchronous processing.
 */
@Singleton
public class CloudPubSubEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(CloudPubSubEmailService.class);
    
    private final Publisher publisher;
    private final ObjectMapper objectMapper;
    private final String projectId;
    private final String sourceName;
    
    @Inject
    public CloudPubSubEmailService(
            @Named("gcp.project.id") String projectId,
            @Named("pubsub.topic.email-events") String topicName,
            @Named("service.name") String sourceName) throws Exception {
        this.projectId = projectId;
        this.sourceName = sourceName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For Java 8 time support
        
        logger.info("Initializing CloudPubSubEmailService - projectId: {}, topicName: {}, sourceName: {}", 
            projectId, topicName, sourceName);
        
        try {
            TopicName topic = TopicName.of(projectId, topicName);
            this.publisher = Publisher.newBuilder(topic)
                .setEnableCompression(true)
                .build();
            
            logger.info("Successfully initialized CloudPubSubEmailService with topic: {}", topic);
        } catch (Exception e) {
            logger.error("Failed to initialize CloudPubSubEmailService", e);
            throw e;
        }
    }
    
    @Override
    public CompletableFuture<Void> sendVerificationEmail(String email, String verificationToken) {
        logger.info("Sending verification email to: {}", email);
        
        String verificationUrl = String.format("%s/verify?token=%s", 
            System.getenv("APP_BASE_URL"), verificationToken);
        
        logger.debug("Verification URL: {}", verificationUrl);
        
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.VERIFICATION_EMAIL)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(email) // Use email as display name if not provided
                .build())
            .templateData(Map.of(
                "actionUrl", verificationUrl,
                "token", verificationToken,
                "expiryTime", Instant.now().plus(24, ChronoUnit.HOURS).toString()
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .priority(EmailMessage.Priority.HIGH)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendPasswordResetEmail(String email, String resetToken) {
        String resetUrl = String.format("%s/reset-password?token=%s", 
            System.getenv("APP_BASE_URL"), resetToken);
            
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.PASSWORD_RESET)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(email)
                .build())
            .templateData(Map.of(
                "actionUrl", resetUrl,
                "token", resetToken,
                "expiryTime", Instant.now().plus(1, ChronoUnit.HOURS).toString()
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .priority(EmailMessage.Priority.HIGH)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    // Password changed email not in interface - keeping for future use
    public CompletableFuture<Void> sendPasswordChangedEmail(String email, String name) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.PASSWORD_CHANGED)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
                .build())
            .templateData(Map.of(
                "timestamp", Instant.now().toString(),
                "supportUrl", "https://support.god-in-a-box.com"
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendNewDeviceLoginNotification(String email, String deviceInfo, String ipAddress) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.NEW_DEVICE_LOGIN)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(email)
                .build())
            .templateData(Map.of(
                "deviceInfo", deviceInfo,
                "ipAddress", ipAddress,
                "timestamp", Instant.now().toString(),
                "securityUrl", "https://god-in-a-box.com/security"
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendMfaCode(String email, String code) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.MFA_CODE)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(email)
                .build())
            .templateData(Map.of(
                "code", code,
                "expiryTime", Instant.now().plus(10, ChronoUnit.MINUTES).toString()
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .priority(EmailMessage.Priority.HIGH)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendAccountLockedNotification(String email, int attempts) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.ACCOUNT_LOCKED)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(email)
                .build())
            .templateData(Map.of(
                "attempts", String.valueOf(attempts),
                "reason", String.format("Too many failed login attempts (%d)", attempts),
                "unlockUrl", "https://god-in-a-box.com/unlock",
                "supportEmail", "security@god-in-a-box.com"
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .priority(EmailMessage.Priority.HIGH)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    /**
     * Generic method to send any type of email.
     * This makes the service extensible for future email types.
     */
    public CompletableFuture<Void> sendEmail(
            EmailType emailType,
            String recipientEmail,
            String recipientName,
            Map<String, String> templateData,
            String userId) {
        
        EmailMessage message = EmailMessage.builder()
            .emailType(emailType)
            .recipient(EmailMessage.Recipient.builder()
                .email(recipientEmail)
                .displayName(recipientName)
                .build())
            .templateData(templateData)
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .userId(userId)
                .priority(emailType.isHighPriority() ? 
                    EmailMessage.Priority.HIGH : EmailMessage.Priority.NORMAL)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    private CompletableFuture<Void> publishMessage(EmailMessage message) {
        logger.info("Starting to publish email message - emailType: {}, recipient: {}", 
            message.getEmailType(), message.getRecipient().getEmail());
            
        return CompletableFuture.runAsync(() -> {
            try {
                // Validate message
                message.validate();
                logger.debug("Message validation passed");
                
                // Convert to JSON
                String json = objectMapper.writeValueAsString(message);
                logger.info("Message JSON being sent to Pub/Sub: {}", json);
                
                // Create Pub/Sub message
                ByteString data = ByteString.copyFromUtf8(json);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(data)
                    .putAttributes("emailType", message.getEmailType().name())
                    .putAttributes("priority", message.getMetadata().getPriority().name())
                    .putAttributes("source", message.getMetadata().getSource())
                    .putAttributes("correlationId", message.getMetadata().getCorrelationId())
                    .build();
                
                logger.info("Publishing to Pub/Sub topic...");
                
                // Publish
                ApiFuture<String> future = publisher.publish(pubsubMessage);
                String messageId = future.get();
                
                logger.info("Successfully published email message: messageId={}, emailType={}, recipient={}, correlationId={}",
                    messageId, message.getEmailType(), message.getRecipient().getEmail(),
                    message.getMetadata().getCorrelationId());
                    
            } catch (Exception e) {
                logger.error("Failed to publish email message: emailType={}, recipient={}, error={}",
                    message.getEmailType(), message.getRecipient().getEmail(), e.getMessage(), e);
                throw new CompletionException(e);
            }
        });
    }
    
    
    /**
     * Cleanup method to be called on shutdown
     */
    public void shutdown() {
        try {
            publisher.shutdown();
        } catch (Exception e) {
            logger.error("Error shutting down publisher", e);
        }
    }
}