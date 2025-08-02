package com.deusexmachina.email.processor;

import java.util.Map;

/**
 * Pub/Sub message model for Cloud Functions.
 */
public class PubsubMessage {
    /**
     * The message body data (base64 encoded).
     */
    public String data;
    
    /**
     * Message attributes.
     */
    public Map<String, String> attributes;
    
    /**
     * Message ID.
     */
    public String messageId;
    
    /**
     * Publish time.
     */
    public String publishTime;
}