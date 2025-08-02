package com.deusexmachina.novel.ai.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.deusexmachina.novel.ai.controller.GenerationController;
import com.deusexmachina.novel.ai.service.AIGenerationService;
import com.deusexmachina.novel.ai.service.impl.GeminiGenerationService;

public class NovelAIModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // Bind services
        bind(AIGenerationService.class).to(GeminiGenerationService.class).in(Singleton.class);
        bind(GenerationController.class).in(Singleton.class);
    }
    
    @Provides
    @Singleton
    Config provideConfig() {
        // Load test configuration if specified
        String configFile = System.getProperty("config.file");
        if (configFile != null) {
            return ConfigFactory.parseFile(new java.io.File(configFile))
                    .withFallback(ConfigFactory.load());
        }
        return ConfigFactory.load();
    }
    
    @Provides
    @Singleton
    VertexAIConfig provideVertexAIConfig(Config config) {
        return VertexAIConfig.builder()
                .projectId(getEnvOrConfig(config, "GCP_PROJECT_ID", "gcp.project.id"))
                .location(config.getString("vertexai.location"))
                .endpoint(config.getString("vertexai.endpoint"))
                .modelNamePro(config.getString("gemini.model.pro"))
                .modelNameFlash(config.getString("gemini.model.flash"))
                .temperatureCreative(config.getDouble("gemini.temperature.creative"))
                .temperatureBalanced(config.getDouble("gemini.temperature.balanced"))
                .temperatureFocused(config.getDouble("gemini.temperature.focused"))
                .maxOutputTokens(config.getInt("gemini.maxOutputTokens"))
                .topK(config.getInt("gemini.topK"))
                .topP(config.getDouble("gemini.topP"))
                .build();
    }
    
    @Provides
    @Singleton
    GenerationConfig provideGenerationConfig(Config config) {
        return GenerationConfig.builder()
                .maxContextTokens(config.getInt("generation.context.maxTokens"))
                .reservedTokens(config.getInt("generation.context.reservedTokens"))
                .systemPromptPath(config.getString("generation.prompts.systemPromptPath"))
                .templatePath(config.getString("generation.prompts.templatePath"))
                .maxRetryAttempts(config.getInt("generation.retry.maxAttempts"))
                .initialRetryDelay(config.getLong("generation.retry.initialDelay"))
                .maxRetryDelay(config.getLong("generation.retry.maxDelay"))
                .retryMultiplier(config.getDouble("generation.retry.multiplier"))
                .timeoutSeconds(config.getInt("generation.timeout.seconds"))
                .build();
    }
    
    private String getEnvOrConfig(Config config, String envKey, String configPath) {
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : config.getString(configPath);
    }
}