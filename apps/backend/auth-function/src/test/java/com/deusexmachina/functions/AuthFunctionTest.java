package com.deusexmachina.functions;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class AuthFunctionTest {
    @Mock private HttpRequest mockRequest;
    @Mock private HttpResponse mockResponse;
    
    private AuthFunction function;
    private StringWriter responseWriter;
    private BufferedWriter bufferedWriter;
    private Gson gson = new Gson();
    
    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        function = new AuthFunction();
        
        // Set up response writer
        responseWriter = new StringWriter();
        bufferedWriter = new BufferedWriter(responseWriter);
        when(mockResponse.getWriter()).thenReturn(bufferedWriter);
    }
    
    @Test
    public void testOptionsRequest() throws Exception {
        when(mockRequest.getMethod()).thenReturn("OPTIONS");
        
        function.service(mockRequest, mockResponse);
        
        verify(mockResponse).appendHeader("Access-Control-Allow-Origin", "*");
        verify(mockResponse).appendHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        verify(mockResponse).appendHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        verify(mockResponse).setStatusCode(204);
    }
    
    @Test
    public void testLoginSuccess() throws Exception {
        // Set up request
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getPath()).thenReturn("/auth/login");
        
        AuthFunction.LoginRequest loginRequest = new AuthFunction.LoginRequest();
        loginRequest.username = "admin";
        loginRequest.password = "password";
        
        StringReader stringReader = new StringReader(gson.toJson(loginRequest));
        BufferedReader bufferedReader = new BufferedReader(stringReader);
        when(mockRequest.getReader()).thenReturn(bufferedReader);
        
        // Execute
        function.service(mockRequest, mockResponse);
        
        // Verify response
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setStatusCode(200);
        
        bufferedWriter.flush();
        String responseBody = responseWriter.toString();
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        
        assertThat(response.has("token")).isTrue();
        assertThat(response.has("refreshToken")).isTrue();
        assertThat(response.get("expiresIn").getAsInt()).isEqualTo(3600);
    }
    
    @Test
    public void testLoginInvalidCredentials() throws Exception {
        // Set up request
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getPath()).thenReturn("/auth/login");
        
        AuthFunction.LoginRequest loginRequest = new AuthFunction.LoginRequest();
        loginRequest.username = "invalid";
        loginRequest.password = "wrong";
        
        StringReader stringReader = new StringReader(gson.toJson(loginRequest));
        BufferedReader bufferedReader = new BufferedReader(stringReader);
        when(mockRequest.getReader()).thenReturn(bufferedReader);
        
        // Execute
        function.service(mockRequest, mockResponse);
        
        // Verify response
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setStatusCode(401);
        
        bufferedWriter.flush();
        String responseBody = responseWriter.toString();
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        
        assertThat(response.get("error").getAsString()).isEqualTo("Invalid credentials");
        assertThat(response.get("statusCode").getAsInt()).isEqualTo(401);
    }
    
    @Test
    public void testLoginMissingData() throws Exception {
        // Set up request with missing password
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getPath()).thenReturn("/auth/login");
        
        JsonObject invalidRequest = new JsonObject();
        invalidRequest.addProperty("username", "admin");
        // password is missing
        
        StringReader stringReader = new StringReader(invalidRequest.toString());
        BufferedReader bufferedReader = new BufferedReader(stringReader);
        when(mockRequest.getReader()).thenReturn(bufferedReader);
        
        // Execute
        function.service(mockRequest, mockResponse);
        
        // Verify response
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setStatusCode(400);
        
        bufferedWriter.flush();
        String responseBody = responseWriter.toString();
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        
        assertThat(response.get("error").getAsString()).isEqualTo("Invalid request body");
    }
    
    @Test
    public void testVerifyMissingToken() throws Exception {
        // Set up request
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getPath()).thenReturn("/auth/verify");
        when(mockRequest.getFirstHeader("Authorization")).thenReturn(Optional.empty());
        
        // Execute
        function.service(mockRequest, mockResponse);
        
        // Verify response
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setStatusCode(401);
        
        bufferedWriter.flush();
        String responseBody = responseWriter.toString();
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        
        assertThat(response.get("error").getAsString()).isEqualTo("Missing or invalid authorization header");
    }
    
    @Test
    public void testInvalidEndpoint() throws Exception {
        // Set up request
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getPath()).thenReturn("/auth/invalid");
        
        // Execute
        function.service(mockRequest, mockResponse);
        
        // Verify response
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setStatusCode(404);
        
        bufferedWriter.flush();
        String responseBody = responseWriter.toString();
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        
        assertThat(response.get("error").getAsString()).isEqualTo("Endpoint not found");
    }
    
    @Test
    public void testMethodNotAllowed() throws Exception {
        // Set up request
        when(mockRequest.getMethod()).thenReturn("DELETE");
        when(mockRequest.getPath()).thenReturn("/auth/login");
        
        // Execute
        function.service(mockRequest, mockResponse);
        
        // Verify response
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setStatusCode(405);
        
        bufferedWriter.flush();
        String responseBody = responseWriter.toString();
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
        
        assertThat(response.get("error").getAsString()).isEqualTo("Method not allowed");
    }
}