package com.deusexmachina.email.processor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Email message model matching the Pub/Sub message format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailMessage {
    
    @Builder.Default
    private String messageId = UUID.randomUUID().toString();
    
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    private EmailType emailType;
    
    private Recipient recipient;
    
    private Sender sender;
    
    private Map<String, String> templateData;
    
    private Metadata metadata;
    
    private List<Attachment> attachments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient {
        private String email;
        private String displayName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sender {
        @Builder.Default
        private String email = "noreply@deusexmachina.app";
        @Builder.Default
        private String name = "DeusExMachina";
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String userId;
        @Builder.Default
        private String correlationId = UUID.randomUUID().toString();
        @Builder.Default
        private int retryCount = 0;
        private String source;
        @Builder.Default
        private Priority priority = Priority.NORMAL;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String filename;
        private String contentType;
        private String data; // Base64 encoded
    }
    
    public enum Priority {
        HIGH("high"),
        NORMAL("normal"),
        LOW("low");
        
        private final String value;
        
        Priority(String value) {
            this.value = value;
        }
        
        public String toValue() {
            return value;
        }
    }
    
    public void validate() {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        
        if (emailType == null) {
            throw new IllegalArgumentException("Email type is required");
        }
        
        if (templateData == null) {
            throw new IllegalArgumentException("Template data is required");
        }
        
        // Basic email validation
        if (!recipient.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Invalid email address: " + recipient.getEmail());
        }
    }
}