package com.deusexmachina.novel.ai.controller;

import com.deusexmachina.novel.ai.model.GenerationRequest;
import com.deusexmachina.novel.ai.model.GenerationResponse;
import com.deusexmachina.novel.ai.model.GenerationType;
import com.deusexmachina.novel.ai.service.AIGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationControllerTest {
    
    @Mock
    private AIGenerationService generationService;
    
    private GenerationController controller;
    
    @BeforeEach
    void setUp() {
        controller = new GenerationController(generationService);
    }
    
    @Test
    void testGenerateText_Success() {
        // Given
        String generationId = UUID.randomUUID().toString();
        GenerationRequest request = GenerationRequest.builder()
                .prompt("Write a dramatic scene")
                .generationType(GenerationType.SCENE)
                .contextId("context-123")
                .build();
                
        GenerationResponse expectedResponse = GenerationResponse.builder()
                .generationId(generationId)
                .contextId("context-123")
                .generationType(GenerationType.SCENE)
                .generatedText("A dramatic scene unfolds...")
                .tokenCount(100)
                .generationTimeMs(1500)
                .modelUsed("gemini-2.5-pro")
                .build();
                
        when(generationService.generateText(any(GenerationRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(expectedResponse));
        
        // When
        CompletableFuture<GenerationResponse> result = controller.generateText(request);
        
        // Then
        assertThat(result).isCompletedWithValue(expectedResponse);
        verify(generationService).generateText(request);
    }
    
    @Test
    void testGenerateText_ServiceError() {
        // Given
        GenerationRequest request = GenerationRequest.builder()
                .prompt("Write a scene")
                .generationType(GenerationType.SCENE)
                .contextId("context-123")
                .build();
                
        RuntimeException serviceError = new RuntimeException("Service unavailable");
        when(generationService.generateText(any(GenerationRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(serviceError));
        
        // When
        CompletableFuture<GenerationResponse> result = controller.generateText(request);
        
        // Then
        assertThat(result).isCompletedExceptionally();
        assertThatThrownBy(() -> result.join())
                .isInstanceOf(CompletionException.class)
                .hasCause(serviceError);
    }
    
    @Test
    void testValidateRequest_ValidRequest() {
        // Given
        GenerationRequest request = GenerationRequest.builder()
                .prompt("Write a scene")
                .generationType(GenerationType.SCENE)
                .contextId("context-123")
                .build();
        
        when(generationService.validatePrompt("Write a scene")).thenReturn(true);
        
        // When
        boolean isValid = controller.validateRequest(request);
        
        // Then
        assertThat(isValid).isTrue();
        verify(generationService).validatePrompt("Write a scene");
    }
    
    @Test
    void testValidateRequest_InvalidPrompt() {
        // Given
        GenerationRequest request = GenerationRequest.builder()
                .prompt("") // Empty prompt
                .generationType(GenerationType.SCENE)
                .contextId("context-123")
                .build();
        
        when(generationService.validatePrompt("")).thenReturn(false);
        
        // When
        boolean isValid = controller.validateRequest(request);
        
        // Then
        assertThat(isValid).isFalse();
        verify(generationService).validatePrompt("");
    }
    
    @Test
    void testEstimateTokens() {
        // Given
        String text = "This is a test prompt for token estimation";
        when(generationService.estimateTokens(text)).thenReturn(10);
        
        // When
        int tokens = controller.estimateTokens(text);
        
        // Then
        assertThat(tokens).isEqualTo(10);
        verify(generationService).estimateTokens(text);
    }
}