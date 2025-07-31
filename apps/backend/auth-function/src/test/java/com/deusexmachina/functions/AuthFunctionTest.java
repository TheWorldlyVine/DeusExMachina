package com.deusexmachina.functions;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthFunctionTest {
    @Mock private HttpRequest mockRequest;
    @Mock private HttpResponse mockResponse;
    
    private AuthFunction function;
    private Gson gson = new Gson();
    private StringWriter responseWriter;
    private BufferedWriter bufferedWriter;
    
    @Before
    public void setUp() throws IOException {
        function = new AuthFunction();
        responseWriter = new StringWriter();
        bufferedWriter = new BufferedWriter(responseWriter);
        when(mockResponse.getWriter()).thenReturn(bufferedWriter);
    }
    
    @Test
    public void testSuccessfulAuthentication() throws Exception {
        // Arrange
        AuthFunction.AuthRequest request = new AuthFunction.AuthRequest();
        request.username = "admin";
        request.password = "password";
        
        String requestJson = gson.toJson(request);
        BufferedReader reader = new BufferedReader(new StringReader(requestJson));
        
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getReader()).thenReturn(reader);
        
        // Act
        function.service(mockRequest, mockResponse);
        bufferedWriter.flush();
        
        // Assert
        verify(mockResponse).setStatusCode(200);
        verify(mockResponse).setContentType("application/json");
        
        String responseJson = responseWriter.toString();
        AuthFunction.AuthResponse response = gson.fromJson(responseJson, AuthFunction.AuthResponse.class);
        
        assertThat(response.token).isNotNull();
        assertThat(response.username).isEqualTo("admin");
        assertThat(response.expiresIn).isGreaterThan(0);
    }
    
    @Test
    public void testInvalidCredentials() throws Exception {
        // Arrange
        AuthFunction.AuthRequest request = new AuthFunction.AuthRequest();
        request.username = "invalid";
        request.password = "wrong";
        
        String requestJson = gson.toJson(request);
        BufferedReader reader = new BufferedReader(new StringReader(requestJson));
        
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getReader()).thenReturn(reader);
        
        // Act
        function.service(mockRequest, mockResponse);
        bufferedWriter.flush();
        
        // Assert
        verify(mockResponse).setStatusCode(401);
        assertThat(responseWriter.toString()).contains("Invalid credentials");
    }
    
    @Test
    public void testOptionsRequest() throws Exception {
        // Arrange
        when(mockRequest.getMethod()).thenReturn("OPTIONS");
        
        // Act
        function.service(mockRequest, mockResponse);
        
        // Assert
        verify(mockResponse).setStatusCode(204);
        verify(mockResponse).appendHeader("Access-Control-Allow-Origin", "*");
        verify(mockResponse).appendHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
    }
    
    @Test
    public void testMethodNotAllowed() throws Exception {
        // Arrange
        when(mockRequest.getMethod()).thenReturn("GET");
        
        // Act
        function.service(mockRequest, mockResponse);
        bufferedWriter.flush();
        
        // Assert
        verify(mockResponse).setStatusCode(405);
        assertThat(responseWriter.toString()).contains("Method not allowed");
    }
    
    @Test
    public void testInvalidRequestFormat() throws Exception {
        // Arrange
        String invalidJson = "{ invalid json }";
        BufferedReader reader = new BufferedReader(new StringReader(invalidJson));
        
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getReader()).thenReturn(reader);
        
        // Act
        function.service(mockRequest, mockResponse);
        bufferedWriter.flush();
        
        // Assert
        verify(mockResponse).setStatusCode(500);
        assertThat(responseWriter.toString()).contains("Internal server error");
    }
}