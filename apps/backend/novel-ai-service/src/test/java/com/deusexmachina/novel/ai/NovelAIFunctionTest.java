package com.deusexmachina.novel.ai;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NovelAIFunctionTest {
    
    private NovelAIFunction function;
    private Gson gson;
    
    @Mock
    private HttpRequest request;
    
    @Mock
    private HttpResponse response;
    
    @BeforeEach
    void setUp() {
        // Set system property to use test configuration
        System.setProperty("config.file", "src/test/resources/application-test.conf");
        function = new NovelAIFunction();
        gson = new Gson();
    }
    
    @Test
    void testHealthCheck() throws IOException {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getPath()).thenReturn("/health");
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setStatusCode(200);
        verify(response).setContentType("application/json");
        
        String responseBody = responseWriter.toString();
        Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
        
        assertThat(responseMap).containsKey("status");
        assertThat(responseMap.get("status")).isEqualTo("healthy");
        assertThat(responseMap).containsKey("service");
        assertThat(responseMap.get("service")).isEqualTo("novel-ai-service");
    }
    
    @Test
    void testGenerateTextEndpoint() throws IOException {
        // Given
        String requestBody = """
            {
                "prompt": "Write a story about a dragon",
                "generationType": "SCENE",
                "contextId": "test-context-123",
                "parameters": {
                    "temperature": 0.7,
                    "maxTokens": 1000
                }
            }
            """;
            
        when(request.getMethod()).thenReturn("POST");
        when(request.getPath()).thenReturn("/generate");
        when(request.getContentType()).thenReturn(Optional.of("application/json"));
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBody)));
        when(request.getFirstHeader("Authorization")).thenReturn(Optional.of("Bearer test-token"));
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setContentType("application/json");
        // For now, we expect a 501 Not Implemented as we haven't built the service yet
        verify(response).setStatusCode(501);
    }
    
    @Test
    void testInvalidMethod() throws IOException {
        // Given
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getPath()).thenReturn("/generate");
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setStatusCode(405);
        verify(response).setContentType("application/json");
        
        String responseBody = responseWriter.toString();
        Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
        
        assertThat(responseMap).containsKey("error");
        assertThat(responseMap.get("error")).isEqualTo("Method not allowed");
    }
    
    @Test
    void testCorsHeaders() throws IOException {
        // Given
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getPath()).thenReturn("/generate");
        when(request.getFirstHeader("Origin")).thenReturn(Optional.of("https://app.deusexmachina.com"));
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setStatusCode(204);
        verify(response).appendHeader("Access-Control-Allow-Origin", "https://app.deusexmachina.com");
        verify(response).appendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        verify(response).appendHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID");
        verify(response).appendHeader("Access-Control-Max-Age", "3600");
    }
    
    @Test
    void testMissingContentType() throws IOException {
        // Given
        when(request.getMethod()).thenReturn("POST");
        when(request.getPath()).thenReturn("/generate");
        when(request.getContentType()).thenReturn(Optional.empty());
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setStatusCode(400);
        verify(response).setContentType("application/json");
        
        String responseBody = responseWriter.toString();
        Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
        
        assertThat(responseMap).containsKey("error");
        assertThat(responseMap.get("error")).isEqualTo("Content-Type must be application/json");
    }
}