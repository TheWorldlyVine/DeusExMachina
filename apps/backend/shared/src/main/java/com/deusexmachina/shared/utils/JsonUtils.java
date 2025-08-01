package com.deusexmachina.shared.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for JSON operations using Gson.
 * Provides common JSON serialization/deserialization functionality.
 */
public final class JsonUtils {
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create();
    
    private static final Gson GSON_COMPACT = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create();
    
    private JsonUtils() {
        // Prevent instantiation
    }
    
    /**
     * Converts an object to JSON string with pretty printing.
     * 
     * @param object the object to convert
     * @return JSON string representation
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }
    
    /**
     * Converts an object to compact JSON string.
     * 
     * @param object the object to convert
     * @return compact JSON string representation
     */
    public static String toCompactJson(Object object) {
        return GSON_COMPACT.toJson(object);
    }
    
    /**
     * Parses JSON string to specified type.
     * 
     * @param json the JSON string
     * @param classOfT the class of T
     * @param <T> the type of the desired object
     * @return an object of type T from the string
     * @throws JsonSyntaxException if json is not valid
     */
    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return GSON.fromJson(json, classOfT);
    }
    
    /**
     * Parses JSON from a Reader to specified type.
     * 
     * @param reader the reader containing JSON
     * @param classOfT the class of T
     * @param <T> the type of the desired object
     * @return an object of type T from the reader
     * @throws JsonSyntaxException if json is not valid
     */
    public static <T> T fromJson(Reader reader, Class<T> classOfT) throws JsonSyntaxException {
        return GSON.fromJson(reader, classOfT);
    }
    
    /**
     * Creates a standard error response JSON object.
     * 
     * @param errorCode the error code
     * @param message the error message
     * @param statusCode the HTTP status code
     * @return JsonObject containing error information
     */
    public static JsonObject createErrorResponse(String errorCode, String message, int statusCode) {
        JsonObject error = new JsonObject();
        error.addProperty("errorCode", errorCode);
        error.addProperty("message", message);
        error.addProperty("statusCode", statusCode);
        error.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return error;
    }
    
    /**
     * Creates a standard success response JSON object.
     * 
     * @param data the data object to include in response
     * @return JsonObject containing success response
     */
    public static JsonObject createSuccessResponse(Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", GSON.toJsonTree(data));
        response.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }
    
    /**
     * Validates if a string is valid JSON.
     * 
     * @param jsonString the string to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString);
            return true;
        } catch (JsonSyntaxException | IllegalStateException e) {
            return false;
        }
    }
    
    /**
     * Gets the shared Gson instance for use across the application.
     * This instance is configured with pretty printing and ISO date format.
     * 
     * @return the shared Gson instance
     */
    public static Gson getGson() {
        return GSON;
    }
}