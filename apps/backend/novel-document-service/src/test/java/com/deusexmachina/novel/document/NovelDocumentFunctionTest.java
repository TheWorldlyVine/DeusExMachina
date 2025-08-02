package com.deusexmachina.novel.document;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.*;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NovelDocumentFunctionTest {
    
    private NovelDocumentFunction function;
    private Gson gson;
    
    @Mock
    private HttpRequest request;
    
    @Mock
    private HttpResponse response;
    
    @BeforeEach
    void setUp() {
        System.setProperty("config.file", "src/test/resources/application-test.conf");
        function = new NovelDocumentFunction();
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
        assertThat(responseMap.get("service")).isEqualTo("novel-document-service");
    }
    
    @Test
    void testCreateDocumentEndpoint() throws IOException {
        // Given
        String requestBody = """
            {
                "contextId": "context-123",
                "title": "The Great Adventure",
                "authorId": "author-001",
                "authorName": "John Doe",
                "description": "An epic tale of adventure",
                "genre": "Fantasy",
                "tags": ["adventure", "fantasy", "epic"]
            }
            """;
            
        when(request.getMethod()).thenReturn("POST");
        when(request.getPath()).thenReturn("/document");
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
        // For now, we expect a 501 Not Implemented
        verify(response).setStatusCode(501);
    }
    
    @Test
    void testGetDocumentEndpoint() throws IOException {
        // Given
        when(request.getMethod()).thenReturn("GET");
        when(request.getPath()).thenReturn("/document/doc-123");
        when(request.getFirstHeader("Authorization")).thenReturn(Optional.of("Bearer test-token"));
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setContentType("application/json");
        // For now, we expect a 501 Not Implemented
        verify(response).setStatusCode(501);
    }
    
    @Test
    void testCorsHeaders() throws IOException {
        // Given
        when(request.getMethod()).thenReturn("OPTIONS");
        when(request.getPath()).thenReturn("/document");
        when(request.getFirstHeader("Origin")).thenReturn(Optional.of("https://app.deusexmachina.com"));
        
        StringWriter responseWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(responseWriter);
        when(response.getWriter()).thenReturn(bufferedWriter);
        
        // When
        function.service(request, response);
        
        // Then
        verify(response).setStatusCode(204);
        verify(response).appendHeader("Access-Control-Allow-Origin", "https://app.deusexmachina.com");
        verify(response).appendHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        verify(response).appendHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Request-ID");
    }
}