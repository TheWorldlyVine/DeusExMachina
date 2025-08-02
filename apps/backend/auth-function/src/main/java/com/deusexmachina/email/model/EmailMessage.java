package com.deusexmachina.email.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an email message to be sent via the email service.
 * This is the core model for all email types in the system.
 */
@Data
@Builder
public class EmailMessage {
    @NonNull
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();
    
    @NonNull
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp = Instant.now();
    
    @NonNull
    private EmailType emailType;
    
    @NonNull
    private Recipient recipient;
    
    /**
     * Optional custom sender. If null, system default will be used.
     */
    private Sender sender;
    
    @NonNull
    private Map<String, String> templateData;
    
    @NonNull
    private Metadata metadata;
    
    /**
     * Optional attachments
     */
    private Attachment[] attachments;
    
    @Data
    @Builder
    public static class Recipient {
        @NonNull
        private String email;
        private String displayName;
        
        public void validate() {
            Preconditions.checkArgument(
                email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$"),
                "Invalid email address"
            );
        }
    }
    
    @Data
    @Builder
    public static class Sender {
        @NonNull
        private String email;
        private String name;
    }
    
    @Data
    @Builder
    public static class Metadata {
        private String userId;
        
        @NonNull
        @Builder.Default
        private String correlationId = UUID.randomUUID().toString();
        
        @Builder.Default
        private int retryCount = 0;
        
        @NonNull
        private String source;
        
        @NonNull
        @Builder.Default
        private Priority priority = Priority.NORMAL;
    }
    
    @Data
    @Builder
    public static class Attachment {
        @NonNull
        private String filename;
        
        @NonNull
        private String contentType;
        
        @NonNull
        private String data; // Base64 encoded
        
        public void validate() {
            Preconditions.checkArgument(
                data.length() < 10 * 1024 * 1024, // 10MB limit
                "Attachment too large"
            );
        }
    }
    
    public enum Priority {
        HIGH,
        NORMAL,
        LOW;
        
        @JsonValue
        public String toValue() {
            return name().toLowerCase();
        }
    }
    
    /**
     * Validates the entire message
     */
    public void validate() {
        recipient.validate();
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                attachment.validate();
            }
        }
    }
}