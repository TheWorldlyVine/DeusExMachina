#!/bin/bash
set -e

# Test script to generate the JSON that would be sent to Pub/Sub

cat > /tmp/TestEmailJson.java << 'EOF'
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class TestEmailJson {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        // Simulate the EmailMessage structure
        var message = Map.of(
            "messageId", UUID.randomUUID().toString(),
            "timestamp", Instant.now(),
            "emailType", "VERIFICATION_EMAIL",
            "recipient", Map.of(
                "email", "test@example.com",
                "displayName", "test@example.com"
            ),
            "sender", null,
            "templateData", Map.of(
                "actionUrl", "https://app.deusexmachina.com/verify?token=test-token",
                "token", "test-token",
                "expiryTime", Instant.now().plus(24, ChronoUnit.HOURS).toString()
            ),
            "metadata", Map.of(
                "userId", null,
                "correlationId", UUID.randomUUID().toString(),
                "retryCount", 0,
                "source", "auth-function",
                "priority", "HIGH"
            ),
            "attachments", null
        );
        
        String json = mapper.writeValueAsString(message);
        System.out.println("Generated JSON:");
        System.out.println(json);
        
        // Pretty print
        System.out.println("\nPretty printed:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message));
    }
}
EOF

cd /tmp
javac -cp ".:/usr/share/java/*" TestEmailJson.java
java -cp ".:/usr/share/java/*" TestEmailJson