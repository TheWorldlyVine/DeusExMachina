package com.deusexmachina.email.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailMessageSerializationTest {
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Test
    void testSerializationMatchesAvroSchema() throws Exception {
        // Build an email message
        EmailMessage message = EmailMessage.builder()
                .emailType(EmailType.VERIFICATION_EMAIL)
                .recipient(EmailMessage.Recipient.builder()
                        .email("test@example.com")
                        .displayName("Test User")
                        .build())
                .templateData(Map.of(
                        "token", "test-token",
                        "actionUrl", "https://example.com/verify?token=test-token",
                        "expiryTime", Instant.now().toString()
                ))
                .metadata(EmailMessage.Metadata.builder()
                        .source("test-service")
                        .priority(EmailMessage.Priority.HIGH)
                        .build())
                .build();
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(message);
        System.out.println("Serialized JSON: " + json);
        
        // Parse back to verify structure
        Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);
        
        // Verify required fields exist
        assertNotNull(jsonMap.get("messageId"));
        assertNotNull(jsonMap.get("timestamp"));
        assertEquals("VERIFICATION_EMAIL", jsonMap.get("emailType"));
        
        // Verify timestamp is a string
        assertTrue(jsonMap.get("timestamp") instanceof String);
        String timestamp = (String) jsonMap.get("timestamp");
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
        
        // Verify metadata
        Map<String, Object> metadata = (Map<String, Object>) jsonMap.get("metadata");
        assertNotNull(metadata);
        assertEquals("test-service", metadata.get("source"));
        assertEquals("high", metadata.get("priority")); // Should be lowercase
        assertNotNull(metadata.get("correlationId"));
        assertEquals(0, metadata.get("retryCount"));
    }
    
    @Test
    void testPrioritySerializesAsLowercase() throws Exception {
        String highJson = objectMapper.writeValueAsString(EmailMessage.Priority.HIGH);
        assertEquals("\"high\"", highJson);
        
        String normalJson = objectMapper.writeValueAsString(EmailMessage.Priority.NORMAL);
        assertEquals("\"normal\"", normalJson);
        
        String lowJson = objectMapper.writeValueAsString(EmailMessage.Priority.LOW);
        assertEquals("\"low\"", lowJson);
    }
}