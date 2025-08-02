import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class TestEmailJson {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Test Priority enum serialization
        System.out.println("Testing Priority enum:");
        System.out.println("HIGH: " + mapper.writeValueAsString(Priority.HIGH));
        System.out.println("NORMAL: " + mapper.writeValueAsString(Priority.NORMAL));
        System.out.println("LOW: " + mapper.writeValueAsString(Priority.LOW));
        
        // Test Instant serialization with JsonFormat
        System.out.println("\nTesting Instant serialization:");
        TestClass test = new TestClass();
        System.out.println("With JsonFormat: " + mapper.writeValueAsString(test));
        
        // Simulate the EmailMessage structure
        var message = Map.of(
            "messageId", UUID.randomUUID().toString(),
            "timestamp", Instant.now().toString(), // Manual ISO conversion
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
                "priority", "high" // lowercase
            ),
            "attachments", null
        );
        
        String json = mapper.writeValueAsString(message);
        System.out.println("\nExpected JSON format:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(message));
    }
    
    enum Priority {
        HIGH,
        NORMAL,
        LOW;
        
        @JsonValue
        public String toValue() {
            return name().toLowerCase();
        }
    }
    
    static class TestClass {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        public Instant timestamp = Instant.now();
    }
}