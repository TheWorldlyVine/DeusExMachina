package com.deusexmachina.email.processor.service;

import com.deusexmachina.email.processor.model.EmailType;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service to render email templates.
 */
@Slf4j
public class EmailTemplateService {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a z")
            .withZone(ZoneId.systemDefault());
    
    public String renderTemplate(EmailType emailType, Map<String, String> templateData) {
        String template = getHtmlTemplate(emailType);
        return replacePlaceholders(template, templateData);
    }
    
    public String renderPlainTextTemplate(EmailType emailType, Map<String, String> templateData) {
        String template = getPlainTextTemplate(emailType);
        return replacePlaceholders(template, templateData);
    }
    
    private String replacePlaceholders(String template, Map<String, String> data) {
        String result = template;
        
        // Replace all placeholders
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            
            // Format timestamps if needed
            if (entry.getKey().contains("Time") && isIsoTimestamp(value)) {
                value = formatTimestamp(value);
            }
            
            result = result.replace(placeholder, value);
        }
        
        // Set default values for common placeholders
        result = result.replace("{{displayName}}", data.getOrDefault("displayName", "User"));
        result = result.replace("{{appName}}", "DeusExMachina");
        result = result.replace("{{supportEmail}}", "support@deusexmachina.app");
        
        return result;
    }
    
    private boolean isIsoTimestamp(String value) {
        try {
            Instant.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String formatTimestamp(String isoTimestamp) {
        try {
            Instant instant = Instant.parse(isoTimestamp);
            return DATE_FORMATTER.format(instant);
        } catch (Exception e) {
            return isoTimestamp;
        }
    }
    
    private String getHtmlTemplate(EmailType emailType) {
        String baseTemplate = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background-color: #4F46E5;
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                    }
                    .content {
                        padding: 30px;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 24px;
                        margin: 20px 0;
                        background-color: #4F46E5;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        font-weight: 500;
                    }
                    .button:hover {
                        background-color: #4338CA;
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #4F46E5;
                        text-align: center;
                        padding: 20px;
                        background-color: #f4f4f4;
                        border-radius: 5px;
                        margin: 20px 0;
                        letter-spacing: 4px;
                    }
                    .footer {
                        background-color: #f8f8f8;
                        padding: 20px;
                        text-align: center;
                        font-size: 14px;
                        color: #666;
                    }
                    .warning {
                        background-color: #FEF3C7;
                        border-left: 4px solid #F59E0B;
                        padding: 15px;
                        margin: 20px 0;
                    }
                    .error {
                        background-color: #FEE2E2;
                        border-left: 4px solid #EF4444;
                        padding: 15px;
                        margin: 20px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>{{appName}}</h1>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>This email was sent by {{appName}}. If you didn't request this email, please ignore it.</p>
                        <p>Need help? Contact us at <a href="mailto:{{supportEmail}}">{{supportEmail}}</a></p>
                    </div>
                </div>
            </body>
            </html>
            """;
        
        String content = switch (emailType) {
            case VERIFICATION_EMAIL -> """
                <h2>Welcome to {{appName}}!</h2>
                <p>Hi {{displayName}},</p>
                <p>Thanks for signing up! Please verify your email address to get started.</p>
                <div style="text-align: center;">
                    <a href="{{actionUrl}}" class="button">Verify Email Address</a>
                </div>
                <p>Or use this verification code:</p>
                <div class="code">{{token}}</div>
                <p>This link will expire at {{expiryTime}}.</p>
                """;
                
            case PASSWORD_RESET -> """
                <h2>Reset Your Password</h2>
                <p>Hi {{displayName}},</p>
                <p>We received a request to reset your password. Click the button below to create a new password:</p>
                <div style="text-align: center;">
                    <a href="{{actionUrl}}" class="button">Reset Password</a>
                </div>
                <p>This link will expire at {{expiryTime}}.</p>
                <div class="warning">
                    <p><strong>Note:</strong> If you didn't request this password reset, you can safely ignore this email. Your password won't be changed.</p>
                </div>
                """;
                
            case PASSWORD_CHANGED -> """
                <h2>Password Changed Successfully</h2>
                <p>Hi {{displayName}},</p>
                <p>Your password was changed at {{timestamp}}.</p>
                <div class="error">
                    <p><strong>Security Alert:</strong> If you didn't make this change, your account may be compromised. Please contact our support team immediately.</p>
                </div>
                <p>For your security, we recommend:</p>
                <ul>
                    <li>Using a unique password for {{appName}}</li>
                    <li>Enabling two-factor authentication</li>
                    <li>Reviewing your recent account activity</li>
                </ul>
                """;
                
            case NEW_DEVICE_LOGIN -> """
                <h2>New Device Login Detected</h2>
                <p>Hi {{displayName}},</p>
                <p>We detected a login to your account from a new device:</p>
                <div style="background-color: #f4f4f4; padding: 20px; border-radius: 5px; margin: 20px 0;">
                    <p><strong>Device:</strong> {{deviceInfo}}</p>
                    <p><strong>IP Address:</strong> {{ipAddress}}</p>
                    <p><strong>Time:</strong> {{timestamp}}</p>
                </div>
                <p>If this was you, no action is needed. If you don't recognize this activity:</p>
                <div style="text-align: center;">
                    <a href="{{securityUrl}}" class="button">Secure Your Account</a>
                </div>
                """;
                
            case MFA_CODE -> """
                <h2>Your Verification Code</h2>
                <p>Hi {{displayName}},</p>
                <p>Here's your verification code for {{appName}}:</p>
                <div class="code">{{code}}</div>
                <p>This code will expire at {{expiryTime}}.</p>
                <p>If you didn't request this code, please ignore this email.</p>
                """;
                
            case ACCOUNT_LOCKED -> """
                <h2>Account Security Alert</h2>
                <p>Hi {{displayName}},</p>
                <div class="error">
                    <p><strong>Your account has been locked</strong></p>
                    <p>Reason: {{reason}}</p>
                </div>
                <p>To unlock your account and regain access:</p>
                <div style="text-align: center;">
                    <a href="{{unlockUrl}}" class="button">Unlock Account</a>
                </div>
                <p>If you need immediate assistance, please contact our security team at <a href="mailto:{{supportEmail}}">{{supportEmail}}</a>.</p>
                """;
                
            default -> """
                <h2>{{emailType}}</h2>
                <p>Hi {{displayName}},</p>
                <p>This is an automated message from {{appName}}.</p>
                """;
        };
        
        return String.format(baseTemplate, content);
    }
    
    private String getPlainTextTemplate(EmailType emailType) {
        return switch (emailType) {
            case VERIFICATION_EMAIL -> """
                Welcome to {{appName}}!
                
                Hi {{displayName}},
                
                Thanks for signing up! Please verify your email address to get started.
                
                Click here to verify: {{actionUrl}}
                
                Or use this verification code: {{token}}
                
                This link will expire at {{expiryTime}}.
                """;
                
            case PASSWORD_RESET -> """
                Reset Your Password
                
                Hi {{displayName}},
                
                We received a request to reset your password.
                
                Click here to reset: {{actionUrl}}
                
                This link will expire at {{expiryTime}}.
                
                If you didn't request this password reset, you can safely ignore this email.
                """;
                
            default -> """
                {{emailType}}
                
                Hi {{displayName}},
                
                This is an automated message from {{appName}}.
                """;
        };
    }
}