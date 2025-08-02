# Adding New Email Types

This guide explains how to add new email types to the DeusExMachina email service.

## Overview

The email service is designed to be extensible. Adding a new email type involves:
1. Adding the email type to the enum
2. Creating a method in the email service (optional)
3. Adding the email template
4. Publishing messages from your service

## Step 1: Add Email Type to Enum

Edit `/apps/backend/auth-function/src/main/java/com/deusexmachina/email/model/EmailType.java`:

```java
public enum EmailType {
    // ... existing types ...
    
    // Add your new type in the appropriate category
    ORDER_SHIPPED("Your order has shipped", "transaction"),
    WEEKLY_DIGEST("Your weekly activity digest", "notification"),
    PROMO_CODE("Special offer just for you", "marketing");
    
    // ... rest of enum ...
}
```

Consider:
- Choose the right category: `auth`, `transaction`, `notification`, or `marketing`
- Set appropriate retention period via `getRetentionDays()`
- Mark as high priority if time-sensitive via `isHighPriority()`

## Step 2: Add Template to Application Integration

Edit `/infrastructure/modules/email-service/application-integration-workflow.yaml`:

```yaml
emailTemplates:
  # ... existing templates ...
  
  ORDER_SHIPPED:
    subject: "Your order #{{orderId}} has shipped!"
    htmlBody: |
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          /* Add your styles */
        </style>
      </head>
      <body>
        <div class="container">
          <h2>Your Order Has Shipped!</h2>
          <p>Hi {{displayName}},</p>
          <p>Good news! Your order #{{orderId}} is on its way.</p>
          <p><strong>Tracking Number:</strong> {{trackingNumber}}</p>
          <p><strong>Carrier:</strong> {{carrier}}</p>
          <p><strong>Estimated Delivery:</strong> {{estimatedDelivery}}</p>
          <p><a href="{{trackingUrl}}" class="button">Track Package</a></p>
        </div>
      </body>
      </html>
```

Template variables are replaced with values from `templateData`.

## Step 3: Send Emails from Your Service

### Option A: Use Specific Method (if added to EmailService interface)

```java
@Inject
private EmailService emailService;

public void notifyOrderShipped(Order order) {
    emailService.sendOrderShippedEmail(
        order.getCustomerEmail(),
        order.getCustomerName(),
        order.getId(),
        order.getTrackingNumber(),
        order.getCarrier(),
        order.getEstimatedDelivery()
    ).exceptionally(ex -> {
        logger.error("Failed to send order shipped email", ex);
        return null;
    });
}
```

### Option B: Use Generic Method (recommended for flexibility)

```java
@Inject
private CloudPubSubEmailService emailService;

public void sendWeeklyDigest(User user, ActivitySummary summary) {
    Map<String, String> templateData = Map.of(
        "totalActivities", String.valueOf(summary.getTotalActivities()),
        "topAchievement", summary.getTopAchievement(),
        "weekStartDate", summary.getStartDate().toString(),
        "weekEndDate", summary.getEndDate().toString(),
        "summaryUrl", "https://app.deusexmachina.com/activity/" + user.getId()
    );
    
    emailService.sendEmail(
        EmailType.WEEKLY_DIGEST,
        user.getEmail(),
        user.getDisplayName(),
        templateData,
        user.getId()
    ).exceptionally(ex -> {
        logger.error("Failed to send weekly digest", ex);
        return null;
    });
}
```

## Step 4: Test Your Email Type

### Unit Test Example

```java
@Test
void testSendOrderShippedEmail() {
    // Arrange
    String email = "test@example.com";
    String orderId = "ORD-12345";
    Map<String, String> templateData = Map.of(
        "orderId", orderId,
        "trackingNumber", "1Z999AA1234567890",
        "carrier", "UPS",
        "estimatedDelivery", "2024-01-15"
    );
    
    // Act
    CompletableFuture<Void> result = emailService.sendEmail(
        EmailType.ORDER_SHIPPED,
        email,
        "Test User",
        templateData,
        "user-123"
    );
    
    // Assert
    assertDoesNotThrow(() -> result.get(5, TimeUnit.SECONDS));
    
    // Verify Pub/Sub message was published
    verify(publisher).publish(argThat(message -> {
        String data = message.getData().toStringUtf8();
        return data.contains(orderId) && 
               data.contains("ORDER_SHIPPED");
    }));
}
```

### Integration Test

```bash
# Send test email via gcloud
gcloud pubsub topics publish deusexmachina-email-events \
  --message='{
    "messageId": "test-123",
    "timestamp": "2024-01-01T12:00:00Z",
    "emailType": "ORDER_SHIPPED",
    "recipient": {
      "email": "test@example.com",
      "displayName": "Test User"
    },
    "templateData": {
      "orderId": "ORD-12345",
      "trackingNumber": "1Z999AA1234567890",
      "carrier": "UPS",
      "estimatedDelivery": "2024-01-15",
      "trackingUrl": "https://ups.com/track/1Z999AA1234567890"
    },
    "metadata": {
      "source": "order-service",
      "correlationId": "test-corr-123"
    }
  }' \
  --project=your-project-id
```

## Best Practices

### 1. Template Data Validation

Always validate template data before sending:

```java
private void validateOrderShippedData(Map<String, String> data) {
    Objects.requireNonNull(data.get("orderId"), "orderId is required");
    Objects.requireNonNull(data.get("trackingNumber"), "trackingNumber is required");
    Objects.requireNonNull(data.get("carrier"), "carrier is required");
}
```

### 2. Error Handling

Handle email failures gracefully:

```java
emailService.sendEmail(emailType, email, name, data, userId)
    .whenComplete((result, error) -> {
        if (error != null) {
            // Log error but don't fail the main operation
            logger.error("Email send failed for {}: {}", 
                emailType, error.getMessage());
            
            // Optionally track metrics
            emailFailureCounter.increment(emailType.name());
        } else {
            emailSuccessCounter.increment(emailType.name());
        }
    });
```

### 3. Rate Limiting

For bulk emails, implement rate limiting:

```java
@Component
public class BulkEmailSender {
    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 emails/second
    
    public void sendBulkEmails(List<User> users, EmailType type, 
                              Function<User, Map<String, String>> dataProvider) {
        users.forEach(user -> {
            rateLimiter.acquire(); // Wait if necessary
            
            Map<String, String> data = dataProvider.apply(user);
            emailService.sendEmail(type, user.getEmail(), 
                user.getDisplayName(), data, user.getId());
        });
    }
}
```

### 4. A/B Testing

Support A/B testing for marketing emails:

```java
public void sendMarketingEmail(User user, Campaign campaign) {
    // Determine variant
    String variant = hashUserId(user.getId()) % 2 == 0 ? "A" : "B";
    
    Map<String, String> templateData = new HashMap<>(campaign.getBaseData());
    templateData.put("variant", variant);
    templateData.put("ctaText", campaign.getCtaText(variant));
    templateData.put("ctaUrl", campaign.getCtaUrl(variant));
    
    emailService.sendEmail(
        EmailType.PROMOTIONAL,
        user.getEmail(),
        user.getDisplayName(),
        templateData,
        user.getId()
    );
}
```

## Monitoring

### View Email Metrics

```bash
# Check Pub/Sub metrics
gcloud monitoring metrics list --filter="metric.type:pubsub.googleapis.com/topic/send_message_operation_count AND resource.labels.topic_id:deusexmachina-email-events"

# View Application Integration logs
gcloud logging read "resource.type=application_integration AND severity>=ERROR" --limit 50

# Check dead letter queue
gcloud pubsub subscriptions pull deusexmachina-email-events-dlq-sub --auto-ack --limit=10
```

### Create Alerts

```yaml
# Example alert for high failure rate
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: email-alerts
spec:
  groups:
    - name: email.rules
      rules:
        - alert: HighEmailFailureRate
          expr: |
            rate(email_send_failures_total[5m]) > 0.1
          for: 5m
          annotations:
            summary: "High email failure rate detected"
            description: "Email failure rate is {{ $value }} failures/second"
```

## Troubleshooting

### Common Issues

1. **Template Variable Not Replaced**
   - Check variable name matches exactly (case-sensitive)
   - Ensure value is in templateData map
   - Check for typos in template

2. **Email Not Sent**
   - Check Pub/Sub topic permissions
   - Verify Application Integration is deployed
   - Check dead letter queue for failed messages

3. **Slow Email Delivery**
   - Check Pub/Sub subscription backlog
   - Verify Application Integration scaling settings
   - Consider increasing max instances

### Debug Mode

Enable debug logging:

```java
// In your service
if (logger.isDebugEnabled()) {
    logger.debug("Publishing email: type={}, recipient={}, data={}", 
        emailType, recipientEmail, templateData);
}

// In Application Integration
console.log('Processing email:', JSON.stringify($.messageData, null, 2));
```

## Security Considerations

1. **PII Protection**: Never log full email addresses or sensitive data
2. **Template Injection**: Sanitize user input in templates
3. **Rate Limiting**: Implement per-user rate limits
4. **Unsubscribe**: Include unsubscribe links for marketing emails
5. **Authentication**: Verify sender identity for sensitive emails