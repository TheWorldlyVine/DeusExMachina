package com.deusexmachina.email.processor;

import com.deusexmachina.email.processor.model.EmailMessage;
import com.deusexmachina.email.processor.service.EmailSenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cloud Function to process email messages from Pub/Sub.
 * Uses Gmail API with service account impersonation for sending emails.
 */
@Slf4j
public class EmailProcessorFunction implements BackgroundFunction<PubsubMessage> {
    
    private final ObjectMapper objectMapper;
    private final EmailSenderService emailSenderService;
    
    public EmailProcessorFunction() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        this.emailSenderService = new EmailSenderService();
    }
    
    @Override
    public void accept(PubsubMessage message, Context context) throws Exception {
        try {
            // Decode the Pub/Sub message
            String messageData = new String(
                Base64.getDecoder().decode(message.data), 
                StandardCharsets.UTF_8
            );
            
            log.info("Processing email message from Pub/Sub: {}", context.eventId());
            log.debug("Message data: {}", messageData);
            
            // Parse the email message
            EmailMessage emailMessage = objectMapper.readValue(messageData, EmailMessage.class);
            
            // Validate the message
            emailMessage.validate();
            
            log.info("Sending email - type: {}, recipient: {}, messageId: {}", 
                emailMessage.getEmailType(), 
                emailMessage.getRecipient().getEmail(),
                emailMessage.getMessageId());
            
            // Send the email
            emailSenderService.sendEmail(emailMessage);
            
            log.info("Email sent successfully - messageId: {}, recipient: {}", 
                emailMessage.getMessageId(), 
                emailMessage.getRecipient().getEmail());
            
        } catch (Exception e) {
            log.error("Failed to process email message", e);
            
            // Check if this is a retry
            String retryCount = message.attributes != null ? message.attributes.getOrDefault("retry_count", "0") : "0";
            int retries = Integer.parseInt(retryCount);
            
            if (retries >= 3) {
                log.error("Email failed after {} retries, sending to dead letter queue", retries);
                // The message will automatically go to DLQ after max delivery attempts
            }
            
            // Re-throw to trigger retry
            throw e;
        }
    }
}