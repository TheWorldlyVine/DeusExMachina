package com.deusexmachina.novel.ai.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerationConfig {
    private final int maxContextTokens;
    private final int reservedTokens;
    private final String systemPromptPath;
    private final String templatePath;
    private final int maxRetryAttempts;
    private final long initialRetryDelay;
    private final long maxRetryDelay;
    private final double retryMultiplier;
    private final int timeoutSeconds;
}