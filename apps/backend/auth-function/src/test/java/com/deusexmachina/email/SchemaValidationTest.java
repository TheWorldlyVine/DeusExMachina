package com.deusexmachina.email;

import com.deusexmachina.email.model.EmailMessage;
import com.deusexmachina.email.model.EmailType;
import com.deusexmachina.email.service.CloudPubSubEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaValidationTest {
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Match the exact configuration from CloudPubSubEmailService
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.findAndRegisterModules();
    }
    
    @Test
    void testEmailMessageSerializationMatchesAvroSchema() throws Exception {
        // Build a complete EmailMessage
        EmailMessage message = EmailMessage.builder()
                .emailType(EmailType.VERIFICATION_EMAIL)
                .recipient(EmailMessage.Recipient.builder()
                        .email("test@example.com")
                        .displayName("Test User")
                        .build())
                .templateData(Map.of(
                        "token", "test-token-123",
                        "actionUrl", "https://app.deusexmachina.com/verify?token=test-token-123",
                        "expiryTime", Instant.now().plusSeconds(86400).toString()
                ))
                .metadata(EmailMessage.Metadata.builder()
                        .source("auth-function")
                        .userId("user-123") // Can be null
                        .priority(EmailMessage.Priority.HIGH)
                        .build())
                .build();
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(message);
        System.out.println("Serialized JSON:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(message));
        
        // Parse back to verify structure
        Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);
        
        // Verify all required fields match AVRO schema
        
        // 1. messageId - must be string
        assertTrue(jsonMap.containsKey("messageId"));
        assertTrue(jsonMap.get("messageId") instanceof String);
        
        // 2. timestamp - MUST be ISO 8601 string, not number!
        assertTrue(jsonMap.containsKey("timestamp"));
        Object timestamp = jsonMap.get("timestamp");
        System.out.println("Timestamp type: " + timestamp.getClass());
        System.out.println("Timestamp value: " + timestamp);
        
        // This is the key issue - it should be a string!
        if (timestamp instanceof Number) {
            fail("Timestamp is being serialized as number " + timestamp + " instead of ISO 8601 string!");
        }
        assertTrue(timestamp instanceof String, "Timestamp must be a string");
        
        // 3. emailType - string
        assertEquals("VERIFICATION_EMAIL", jsonMap.get("emailType"));
        
        // 4. recipient - record with email and displayName
        Map<String, Object> recipient = (Map<String, Object>) jsonMap.get("recipient");
        assertEquals("test@example.com", recipient.get("email"));
        assertEquals("Test User", recipient.get("displayName"));
        
        // 5. sender - can be null
        assertTrue(jsonMap.containsKey("sender"));
        
        // 6. templateData - map of strings
        Map<String, String> templateData = (Map<String, String>) jsonMap.get("templateData");
        assertTrue(templateData.containsKey("token"));
        assertTrue(templateData.containsKey("actionUrl"));
        
        // 7. metadata - record with specific fields
        Map<String, Object> metadata = (Map<String, Object>) jsonMap.get("metadata");
        
        // Priority MUST be lowercase!
        Object priority = metadata.get("priority");
        System.out.println("Priority value: " + priority);
        assertEquals("high", priority, "Priority must be lowercase 'high', not 'HIGH'!");
        
        // Other metadata fields
        assertEquals("auth-function", metadata.get("source"));
        assertTrue(metadata.containsKey("correlationId"));
        assertEquals(0, metadata.get("retryCount"));
        
        // 8. attachments - can be null
        assertTrue(jsonMap.containsKey("attachments"));
    }
    
    @Test
    void testInstantSerializationDirectly() throws Exception {
        Instant now = Instant.now();
        String serialized = objectMapper.writeValueAsString(now);
        System.out.println("Direct Instant serialization: " + serialized);
        
        // It should be a quoted string
        assertTrue(serialized.startsWith("\"") && serialized.endsWith("\""));
        
        // Remove quotes and verify it's ISO format
        String unquoted = serialized.substring(1, serialized.length() - 1);
        assertTrue(unquoted.contains("T")); // ISO 8601 has T separator
        assertTrue(unquoted.endsWith("Z")); // UTC timezone
    }
}