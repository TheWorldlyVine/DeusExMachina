package com.deusexmachina.novel.ai.service.impl;

import com.deusexmachina.novel.ai.config.GenerationConfig;
import com.deusexmachina.novel.ai.config.VertexAIConfig;
import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GeminiGenerationServiceTest {
    
    private GeminiGenerationService service;
    private VertexAIConfig vertexAIConfig;
    private GenerationConfig generationConfig;
    
    @BeforeEach
    void setUp() {
        vertexAIConfig = VertexAIConfig.builder()
                .projectId("test-project")
                .location("us-central1")
                .endpoint("test-endpoint")
                .modelNamePro("gemini-2.5-pro")
                .modelNameFlash("gemini-2.5-flash")
                .temperatureCreative(0.9)
                .temperatureBalanced(0.7)
                .temperatureFocused(0.3)
                .maxOutputTokens(8192)
                .topK(40)
                .topP(0.95)
                .build();
                
        generationConfig = GenerationConfig.builder()
                .maxContextTokens(30000)
                .reservedTokens(2000)
                .systemPromptPath("/prompts/system/")
                .templatePath("/prompts/templates/")
                .maxRetryAttempts(3)
                .initialRetryDelay(1000)
                .maxRetryDelay(10000)
                .retryMultiplier(2.0)
                .timeoutSeconds(120)
                .build();
                
        service = new GeminiGenerationService(vertexAIConfig, generationConfig);
    }
    
    @Test
    void testValidatePrompt_ValidPrompt() {
        // Given
        String validPrompt = "Write a story about a brave knight";
        
        // When
        boolean isValid = service.validatePrompt(validPrompt);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    void testValidatePrompt_NullPrompt() {
        // When
        boolean isValid = service.validatePrompt(null);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidatePrompt_EmptyPrompt() {
        // When
        boolean isValid = service.validatePrompt("");
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidatePrompt_BlankPrompt() {
        // When
        boolean isValid = service.validatePrompt("   ");
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testValidatePrompt_TooLongPrompt() {
        // Given - Create a prompt that would exceed token limits
        StringBuilder longPrompt = new StringBuilder();
        for (int i = 0; i < 30000; i++) {
            longPrompt.append("word ");
        }
        
        // When
        boolean isValid = service.validatePrompt(longPrompt.toString());
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    void testEstimateTokens_NullText() {
        // When
        int tokens = service.estimateTokens(null);
        
        // Then
        assertThat(tokens).isEqualTo(0);
    }
    
    @Test
    void testEstimateTokens_EmptyText() {
        // When
        int tokens = service.estimateTokens("");
        
        // Then
        assertThat(tokens).isEqualTo(0);
    }
    
    @Test
    void testEstimateTokens_ShortText() {
        // Given
        String text = "Hello world"; // 11 characters
        
        // When
        int tokens = service.estimateTokens(text);
        
        // Then
        // With our simple estimation (1 token per 4 characters), this should be 3
        assertThat(tokens).isEqualTo(3);
    }
    
    @Test
    void testEstimateTokens_LongerText() {
        // Given
        String text = "The quick brown fox jumps over the lazy dog"; // 44 characters
        
        // When
        int tokens = service.estimateTokens(text);
        
        // Then
        // With our simple estimation, this should be 11
        assertThat(tokens).isEqualTo(11);
    }
    
    @Test
    void testGenerateText_NotImplemented() {
        // Given
        GenerationRequest request = GenerationRequest.builder()
                .prompt("Test prompt")
                .generationType(GenerationType.SCENE)
                .contextId("test-context")
                .build();
        
        // When/Then
        assertThat(service.generateText(request))
                .failsWithin(java.time.Duration.ofSeconds(1))
                .withThrowableOfType(Exception.class);
    }
}