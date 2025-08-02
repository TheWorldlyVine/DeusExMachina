# Auth Email Service Technical Specification

## 1. Introduction

### 1.1 Purpose
This document specifies the design and implementation of the email service for authentication-related communications in the DeusExMachina platform, leveraging Google Cloud Platform's native services while maintaining our custom authentication system.

### 1.2 Scope
This specification covers:
- Email service architecture using GCP-native solutions
- Integration with the existing custom authentication system
- Email types and templates
- Queue-based processing with Cloud Pub/Sub
- Error handling and retry mechanisms
- Monitoring and observability

### 1.3 Goals
- **Zero external dependencies**: Use only GCP-native services for email sending
- **Scalability**: Handle high volumes of auth-related emails
- **Reliability**: Ensure email delivery with proper retry mechanisms
- **Maintainability**: Simple architecture with minimal operational overhead
- **Cost-effectiveness**: Leverage GCP's pay-per-use model

## 2. System Architecture

### 2.1 High-Level Design

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────────────┐
│   Auth Function │────▶│ Cloud Pub/Sub│────▶│ Application         │
│                 │     │   Topic      │     │ Integration         │
└─────────────────┘     └──────────────┘     │ Workflow            │
                                              └──────────┬──────────┘
                                                         │
                                              ┌──────────▼──────────┐
                                              │  Send Email Task    │
                                              │  (Native GCP)       │
                                              └─────────────────────┘
```

### 2.2 Components

#### 2.2.1 Cloud Pub/Sub Topic
- **Name**: `auth-email-events`
- **Message Schema**: JSON with email details
- **Retention**: 7 days
- **Message ordering**: Not required

#### 2.2.2 Application Integration Workflow
- **Service**: GCP Application Integration
- **Trigger**: Cloud Pub/Sub subscription
- **Tasks**: 
  - Parse message
  - Validate email data
  - Send email using native Send Email task
  - Handle errors

#### 2.2.3 Email Service Integration
- Integrated into existing auth function
- Publishes events instead of direct email sending
- Maintains backward compatibility with EmailService interface

### 2.3 Message Schema

```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-01T12:00:00Z",
  "emailType": "VERIFICATION_EMAIL",
  "recipient": {
    "email": "user@example.com",
    "displayName": "John Doe"
  },
  "templateData": {
    "actionUrl": "https://app.deusexmachina.com/verify?token=...",
    "code": "123456",
    "expiryTime": "2024-01-01T13:00:00Z"
  },
  "metadata": {
    "userId": "user-123",
    "correlationId": "auth-request-456",
    "retryCount": 0
  }
}
```

### 2.4 Email Types

| Email Type | Description | Template Variables |
|------------|-------------|-------------------|
| VERIFICATION_EMAIL | Account email verification | actionUrl, code, expiryTime |
| PASSWORD_RESET | Password reset request | actionUrl, code, expiryTime |
| PASSWORD_CHANGED | Password change notification | timestamp, deviceInfo |
| NEW_DEVICE_LOGIN | New device/location login | deviceInfo, location, timestamp |
| MFA_CODE | Multi-factor auth code | code, expiryTime |
| ACCOUNT_LOCKED | Account security lock | reason, unlockUrl, supportEmail |

## 3. Implementation Details

### 3.1 Auth Function Updates

#### 3.1.1 EmailService Interface Updates
```java
public interface EmailService {
    CompletableFuture<Void> sendVerificationEmail(String email, String name, String verificationUrl);
    CompletableFuture<Void> sendPasswordResetEmail(String email, String name, String resetUrl);
    CompletableFuture<Void> sendPasswordChangedEmail(String email, String name);
    CompletableFuture<Void> sendNewDeviceLoginEmail(String email, String name, DeviceInfo device);
    CompletableFuture<Void> sendMfaCodeEmail(String email, String name, String code);
    CompletableFuture<Void> sendAccountLockedEmail(String email, String name, String reason);
}
```

#### 3.1.2 Cloud Pub/Sub Email Service Implementation
```java
@Singleton
public class CloudPubSubEmailService implements EmailService {
    private final Publisher publisher;
    private final String projectId;
    private final ObjectMapper objectMapper;
    
    @Inject
    public CloudPubSubEmailService(
            @Named("gcp.project.id") String projectId,
            @Named("pubsub.topic.auth-emails") String topicName) {
        this.projectId = projectId;
        this.objectMapper = new ObjectMapper();
        
        TopicName topic = TopicName.of(projectId, topicName);
        this.publisher = Publisher.newBuilder(topic).build();
    }
    
    @Override
    public CompletableFuture<Void> sendVerificationEmail(
            String email, String name, String verificationUrl) {
        EmailMessage message = EmailMessage.builder()
            .messageId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .emailType(EmailType.VERIFICATION_EMAIL)
            .recipient(new Recipient(email, name))
            .templateData(Map.of(
                "actionUrl", verificationUrl,
                "expiryTime", Instant.now().plus(24, ChronoUnit.HOURS)
            ))
            .build();
            
        return publishMessage(message);
    }
    
    private CompletableFuture<Void> publishMessage(EmailMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            ByteString data = ByteString.copyFromUtf8(json);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(data)
                .putAttributes("emailType", message.getEmailType().name())
                .build();
                
            ApiFuture<String> future = publisher.publish(pubsubMessage);
            
            return CompletableFuture.runAsync(() -> {
                try {
                    String messageId = future.get();
                    logger.info("Published email message: {} for type: {}", 
                        messageId, message.getEmailType());
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

### 3.2 Application Integration Workflow

#### 3.2.1 Workflow Configuration
```yaml
name: auth-email-processor
description: Process authentication emails from Pub/Sub
trigger:
  type: CloudPubSubTrigger
  subscription: projects/${project_id}/subscriptions/auth-email-events-sub
  
tasks:
  - name: ParseMessage
    type: DataMapping
    input: $trigger.message.data
    output: $emailData
    mapping:
      emailData: "json.parse(base64.decode(input))"
      
  - name: ValidateEmail
    type: Condition
    condition: |
      emailData.recipient.email != null && 
      emailData.emailType != null &&
      emailData.templateData != null
    onTrue: SendEmail
    onFalse: LogError
    
  - name: SendEmail
    type: SendEmail
    config:
      to: "${emailData.recipient.email}"
      from: "noreply@deusexmachina.app"
      fromName: "DeusExMachina"
      subject: "${getEmailSubject(emailData.emailType)}"
      body: "${renderEmailTemplate(emailData.emailType, emailData.templateData)}"
      isHtml: true
    onSuccess: LogSuccess
    onError: HandleError
    
  - name: HandleError
    type: Condition
    condition: "emailData.metadata.retryCount < 3"
    onTrue: RetryEmail
    onFalse: DeadLetter
```

#### 3.2.2 Email Templates

Templates are defined within the Application Integration workflow using the built-in templating system:

```javascript
function renderEmailTemplate(emailType, data) {
  const templates = {
    VERIFICATION_EMAIL: `
      <h2>Welcome to DeusExMachina!</h2>
      <p>Please verify your email address by clicking the link below:</p>
      <a href="${data.actionUrl}" style="...">Verify Email</a>
      <p>Or use this code: <strong>${data.code}</strong></p>
      <p>This link expires at ${data.expiryTime}</p>
    `,
    PASSWORD_RESET: `
      <h2>Password Reset Request</h2>
      <p>Click the link below to reset your password:</p>
      <a href="${data.actionUrl}" style="...">Reset Password</a>
      <p>This link expires at ${data.expiryTime}</p>
      <p>If you didn't request this, please ignore this email.</p>
    `,
    // ... other templates
  };
  
  return templates[emailType] || templates.DEFAULT;
}
```

### 3.3 Error Handling and Retries

#### 3.3.1 Retry Strategy
- Maximum retries: 3
- Backoff strategy: Exponential (1min, 5min, 15min)
- Dead letter queue for failed messages
- Alerts on repeated failures

#### 3.3.2 Dead Letter Processing
```yaml
- name: DeadLetter
    type: CloudFunction
    function: process-dead-letter-emails
    input:
      message: $emailData
      error: $error
      attempts: $emailData.metadata.retryCount
```

### 3.4 Alternative: Gmail API Integration

For organizations with Google Workspace, an alternative implementation using Gmail API:

```java
@Singleton
public class GmailEmailService implements EmailService {
    private final Gmail gmail;
    private final String senderEmail;
    
    @Inject
    public GmailEmailService(
            @Named("gmail.service.account") String serviceAccountPath,
            @Named("gmail.sender.email") String senderEmail) {
        this.senderEmail = senderEmail;
        this.gmail = createGmailService(serviceAccountPath);
    }
    
    @Override
    public CompletableFuture<Void> sendVerificationEmail(
            String email, String name, String verificationUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MimeMessage mimeMessage = createMimeMessage(
                    email, 
                    "Verify your email address",
                    renderVerificationEmail(name, verificationUrl)
                );
                
                Message message = createMessage(mimeMessage);
                gmail.users().messages().send("me", message).execute();
                
                return null;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
}
```

## 4. Deployment and Operations

### 4.1 Infrastructure as Code

#### 4.1.1 Terraform Configuration
```hcl
# Cloud Pub/Sub Topic
resource "google_pubsub_topic" "auth_emails" {
  name = "auth-email-events"
  
  message_retention_duration = "604800s" # 7 days
  
  schema_settings {
    schema = google_pubsub_schema.auth_email_schema.id
    encoding = "JSON"
  }
}

# Subscription with retry policy
resource "google_pubsub_subscription" "auth_emails" {
  name  = "auth-email-events-sub"
  topic = google_pubsub_topic.auth_emails.name
  
  retry_policy {
    minimum_backoff = "60s"
    maximum_backoff = "900s" # 15 minutes
  }
  
  dead_letter_policy {
    dead_letter_topic = google_pubsub_topic.auth_emails_dlq.id
    max_delivery_attempts = 3
  }
  
  expiration_policy {
    ttl = "" # Never expire
  }
}

# Application Integration (configured via gcloud or console)
# Note: Terraform provider for Application Integration is not yet available
```

### 4.2 Monitoring and Observability

#### 4.2.1 Metrics
- Email send success rate
- Email send latency
- Queue depth and processing time
- Error rates by email type
- Retry counts

#### 4.2.2 Logging
```java
// Structured logging for email events
logger.info("Email event published", 
    "messageId", message.getMessageId(),
    "emailType", message.getEmailType(),
    "recipient", message.getRecipient().getEmail(),
    "correlationId", message.getMetadata().getCorrelationId()
);
```

#### 4.2.3 Alerts
- Queue depth > 1000 messages
- Error rate > 5% over 5 minutes
- Dead letter queue has messages
- Application Integration workflow failures

### 4.3 Security Considerations

1. **PII Protection**: Email addresses logged only at INFO level, full content at DEBUG
2. **Encryption**: All messages encrypted in transit (TLS)
3. **Access Control**: Least privilege IAM roles
4. **Template Injection**: Sanitize all user-provided data in templates
5. **Rate Limiting**: Implement per-user email rate limits

## 5. Migration Plan

### 5.1 Phase 1: Deploy Infrastructure
1. Create Pub/Sub topics and subscriptions
2. Deploy Application Integration workflow
3. Update auth function with feature flag

### 5.2 Phase 2: Gradual Rollout
1. Enable for 10% of users
2. Monitor metrics and errors
3. Increase to 50%, then 100%

### 5.3 Phase 3: Cleanup
1. Remove SendGrid configuration
2. Delete old email service code
3. Update documentation

## 6. Cost Analysis

### 6.1 Estimated Monthly Costs (10,000 emails/month)
- Cloud Pub/Sub: ~$0.40
- Application Integration: ~$50 (minimum tier)
- Cloud Functions (dead letter): ~$0.10
- Total: ~$50.50/month

### 6.2 Comparison
- SendGrid: $0 (free tier up to 100/day)
- Current solution: More expensive but fully integrated

## 7. Future Enhancements

1. **Email Analytics**: Track open rates using pixel tracking
2. **Batch Sending**: Group notifications for digest emails
3. **Template Management**: UI for editing email templates
4. **Multi-language Support**: Localized email templates
5. **A/B Testing**: Test different email templates

## 8. Appendices

### 8.1 Example Cloud Function for Dead Letter Processing
```java
public class DeadLetterProcessor implements BackgroundFunction<PubsubMessage> {
    @Override
    public void accept(PubsubMessage message, Context context) {
        String data = new String(
            Base64.getDecoder().decode(message.getData()), 
            StandardCharsets.UTF_8
        );
        
        EmailMessage emailMessage = objectMapper.readValue(data, EmailMessage.class);
        
        // Log to monitoring system
        logger.error("Email failed after retries", 
            "messageId", emailMessage.getMessageId(),
            "emailType", emailMessage.getEmailType(),
            "recipient", emailMessage.getRecipient().getEmail(),
            "attempts", emailMessage.getMetadata().getRetryCount()
        );
        
        // Could also write to Firestore for manual review
        // or trigger an alert to operations team
    }
}
```

### 8.2 Testing Strategy

1. **Unit Tests**: Mock Pub/Sub publisher
2. **Integration Tests**: Use Pub/Sub emulator
3. **Load Tests**: Verify queue performance
4. **End-to-End Tests**: Full workflow validation

### 8.3 References

- [GCP Application Integration Documentation](https://cloud.google.com/application-integration/docs)
- [Cloud Pub/Sub Best Practices](https://cloud.google.com/pubsub/docs/best-practices)
- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [Existing Auth Technical Specification](./authentication-and-authorization.md)