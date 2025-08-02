package com.deusexmachina.novel.ai.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VertexAIConfig {
    private final String projectId;
    private final String location;
    private final String endpoint;
    private final String modelNamePro;
    private final String modelNameFlash;
    private final double temperatureCreative;
    private final double temperatureBalanced;
    private final double temperatureFocused;
    private final int maxOutputTokens;
    private final int topK;
    private final double topP;
}