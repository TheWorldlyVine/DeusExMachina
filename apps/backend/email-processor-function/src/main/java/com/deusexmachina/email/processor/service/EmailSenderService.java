package com.deusexmachina.email.processor.service;

import com.deusexmachina.email.processor.model.EmailMessage;
import com.deusexmachina.email.processor.model.EmailType;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Properties;
import java.util.Map;

/**
 * Service to send emails using SMTP (Gmail).
 * Uses application-specific password for authentication.
 */
@Slf4j
public class EmailSenderService {
    
    private final Session session;
    private final String senderEmail;
    private final String senderName;
    private final EmailTemplateService templateService;
    
    public EmailSenderService() {
        this.senderEmail = System.getenv("SMTP_FROM_EMAIL");
        this.senderName = System.getenv("SMTP_FROM_NAME");
        this.templateService = new EmailTemplateService();
        
        // Configure SMTP properties for Gmail
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        // Create session with authentication
        String username = System.getenv("SMTP_USERNAME");
        String password = System.getenv("SMTP_PASSWORD");
        
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
    
    public void sendEmail(EmailMessage emailMessage) throws MessagingException {
        try {
            // Create message
            MimeMessage message = new MimeMessage(session);
            
            // Set from
            InternetAddress from = new InternetAddress(
                emailMessage.getSender() != null ? emailMessage.getSender().getEmail() : senderEmail,
                emailMessage.getSender() != null ? emailMessage.getSender().getName() : senderName
            );
            message.setFrom(from);
            
            // Set to
            InternetAddress to = new InternetAddress(
                emailMessage.getRecipient().getEmail(),
                emailMessage.getRecipient().getDisplayName()
            );
            message.setRecipient(Message.RecipientType.TO, to);
            
            // Set subject
            String subject = getSubject(emailMessage);
            message.setSubject(subject);
            
            // Create multipart message
            Multipart multipart = new MimeMultipart("alternative");
            
            // Add HTML part
            MimeBodyPart htmlPart = new MimeBodyPart();
            String htmlContent = templateService.renderTemplate(
                emailMessage.getEmailType(), 
                emailMessage.getTemplateData()
            );
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);
            
            // Add plain text part (fallback)
            MimeBodyPart textPart = new MimeBodyPart();
            String textContent = templateService.renderPlainTextTemplate(
                emailMessage.getEmailType(), 
                emailMessage.getTemplateData()
            );
            textPart.setText(textContent, "UTF-8");
            multipart.addBodyPart(textPart);
            
            // Set content
            message.setContent(multipart);
            
            // Add headers
            message.addHeader("X-Priority", 
                emailMessage.getMetadata().getPriority() == EmailMessage.Priority.HIGH ? "1" : "3");
            message.addHeader("X-MessageID", emailMessage.getMessageId());
            message.addHeader("X-CorrelationID", emailMessage.getMetadata().getCorrelationId());
            
            // Send the message
            Transport.send(message);
            
            log.info("Email sent successfully via SMTP - messageId: {}, recipient: {}, subject: {}", 
                emailMessage.getMessageId(), 
                emailMessage.getRecipient().getEmail(),
                subject);
                
        } catch (Exception e) {
            log.error("Failed to send email via SMTP - messageId: {}, recipient: {}", 
                emailMessage.getMessageId(), 
                emailMessage.getRecipient().getEmail(), 
                e);
            throw new MessagingException("Failed to send email", e);
        }
    }
    
    private String getSubject(EmailMessage emailMessage) {
        // Check if subject is provided in template data
        String subject = emailMessage.getTemplateData().get("subject");
        if (subject != null && !subject.isEmpty()) {
            return subject;
        }
        
        // Use default subject for email type
        return emailMessage.getEmailType().getDefaultSubject();
    }
}