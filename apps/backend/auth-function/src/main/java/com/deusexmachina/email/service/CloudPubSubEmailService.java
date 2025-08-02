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
        
        TopicName topic = TopicName.of(projectId, topicName);
        this.publisher = Publisher.newBuilder(topic)
            .setEnableCompression(true)
            .build();
        
        logger.info("Initialized CloudPubSubEmailService with topic: {}", topic);
    }
    
    @Override
    public CompletableFuture<Void> sendVerificationEmail(String email, String name, String verificationUrl) {
        String code = generateVerificationCode();
        
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.VERIFICATION_EMAIL)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
                .build())
            .templateData(Map.of(
                "actionUrl", verificationUrl,
                "code", code,
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
    public CompletableFuture<Void> sendPasswordResetEmail(String email, String name, String resetUrl) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.PASSWORD_RESET)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
                .build())
            .templateData(Map.of(
                "actionUrl", resetUrl,
                "expiryTime", Instant.now().plus(1, ChronoUnit.HOURS).toString()
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .priority(EmailMessage.Priority.HIGH)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendPasswordChangedEmail(String email, String name) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.PASSWORD_CHANGED)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
                .build())
            .templateData(Map.of(
                "timestamp", Instant.now().toString(),
                "supportUrl", "https://support.deusexmachina.com"
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendNewDeviceLoginEmail(String email, String name, String deviceInfo, String location) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.NEW_DEVICE_LOGIN)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
                .build())
            .templateData(Map.of(
                "deviceInfo", deviceInfo,
                "location", location,
                "timestamp", Instant.now().toString(),
                "securityUrl", "https://app.deusexmachina.com/security"
            ))
            .metadata(EmailMessage.Metadata.builder()
                .source(sourceName)
                .build())
            .build();
            
        return publishMessage(message);
    }
    
    @Override
    public CompletableFuture<Void> sendMfaCodeEmail(String email, String name, String code) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.MFA_CODE)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
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
    public CompletableFuture<Void> sendAccountLockedEmail(String email, String name, String reason) {
        EmailMessage message = EmailMessage.builder()
            .emailType(EmailType.ACCOUNT_LOCKED)
            .recipient(EmailMessage.Recipient.builder()
                .email(email)
                .displayName(name)
                .build())
            .templateData(Map.of(
                "reason", reason,
                "unlockUrl", "https://app.deusexmachina.com/unlock",
                "supportEmail", "security@deusexmachina.com"
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
        return CompletableFuture.runAsync(() -> {
            try {
                // Validate message
                message.validate();
                
                // Convert to JSON
                String json = objectMapper.writeValueAsString(message);
                
                // Create Pub/Sub message
                ByteString data = ByteString.copyFromUtf8(json);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(data)
                    .putAttributes("emailType", message.getEmailType().name())
                    .putAttributes("priority", message.getMetadata().getPriority().name())
                    .putAttributes("source", message.getMetadata().getSource())
                    .putAttributes("correlationId", message.getMetadata().getCorrelationId())
                    .build();
                
                // Publish
                ApiFuture<String> future = publisher.publish(pubsubMessage);
                String messageId = future.get();
                
                logger.info("Published email message: messageId={}, emailType={}, recipient={}, correlationId={}",
                    messageId, message.getEmailType(), message.getRecipient().getEmail(),
                    message.getMetadata().getCorrelationId());
                    
            } catch (Exception e) {
                logger.error("Failed to publish email message: emailType={}, recipient={}",
                    message.getEmailType(), message.getRecipient().getEmail(), e);
                throw new CompletionException(e);
            }
        });
    }
    
    private String generateVerificationCode() {
        // Generate 6-digit code
        return String.format("%06d", (int) (Math.random() * 1000000));
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