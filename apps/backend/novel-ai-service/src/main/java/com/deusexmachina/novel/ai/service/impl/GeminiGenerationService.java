package com.deusexmachina.novel.ai.service.impl;

import com.deusexmachina.novel.ai.model.*;
import com.deusexmachina.novel.ai.service.GenerationService;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.*;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Gemini-based implementation of the GenerationService.
 */
@Singleton
public class GeminiGenerationService implements GenerationService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiGenerationService.class);
    
    private final VertexAI vertexAI;
    private final String projectId;
    private final String location;
    
    @Inject
    public GeminiGenerationService() {
        this.projectId = System.getenv("GCP_PROJECT_ID");
        this.location = System.getenv("GCP_REGION") != null ? System.getenv("GCP_REGION") : "us-central1";
        
        // Validate project ID
        if (this.projectId == null || this.projectId.isEmpty()) {
            logger.error("GCP_PROJECT_ID environment variable is not set!");
            throw new IllegalStateException("GCP_PROJECT_ID environment variable is required");
        }
        
        // Initialize Vertex AI
        this.vertexAI = new VertexAI(projectId, location);
        logger.info("Initialized Gemini Generation Service for project: {} in location: {}", projectId, location);
    }
    
    @Override
    public CompletableFuture<GenerationResponse> generateText(GenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateTextSync(request);
            } catch (GenerationException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public GenerationResponse generateTextSync(GenerationRequest request) throws GenerationException {
        logger.info("Generating text for type: {} with prompt length: {}", 
            request.getGenerationType(), request.getPrompt().length());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Select model based on generation type and preference
            String modelName = selectModel(request);
            GenerativeModel model = new GenerativeModel(modelName, vertexAI);
            
            // Configure generation parameters
            GenerationParameters params = request.getParameters() != null ? 
                request.getParameters() : GenerationParameters.builder().build();
            GenerativeModel configuredModel = model.withGenerationConfig(buildGenerationConfig(params))
                .withSafetySettings(buildSafetySettings(params.getSafetyLevel()));
            
            // Build the prompt with context
            String enhancedPrompt = buildEnhancedPrompt(request);
            logger.info("Enhanced prompt: {}", enhancedPrompt);
            
            // Count tokens
            int promptTokens = countTokens(enhancedPrompt);
            
            // Generate content
            Content content = ContentMaker.fromString(enhancedPrompt);
            GenerateContentResponse response = configuredModel.generateContent(content);
            
            // Log response details
            logger.info("Response candidates count: {}", 
                response.getCandidatesList() != null ? response.getCandidatesList().size() : "null");
            if (response.getCandidatesList() != null && !response.getCandidatesList().isEmpty()) {
                var candidate = response.getCandidatesList().get(0);
                logger.info("Candidate finish reason: {}", candidate.getFinishReason());
                logger.info("Candidate content parts count: {}", 
                    candidate.getContent() != null && candidate.getContent().getPartsList() != null ? 
                    candidate.getContent().getPartsList().size() : "null");
            }
            
            // Extract generated text
            String generatedText = ResponseHandler.getText(response);
            
            // If no text generated, check if content was blocked
            if (generatedText == null || generatedText.isEmpty()) {
                logger.warn("No text generated. Checking for blocked content or other issues...");
                
                // Check if prompt filters blocked the content
                if (response.getPromptFeedback() != null) {
                    logger.warn("Prompt feedback: {}", response.getPromptFeedback());
                    if (response.getPromptFeedback().getBlockReason() != null) {
                        throw new GenerationException("Content blocked by safety filters: " + 
                            response.getPromptFeedback().getBlockReason());
                    }
                }
                
                // Provide a fallback response
                generatedText = "I need more specific details to generate a scene. Please provide:\n" +
                    "- Character names and their roles\n" +
                    "- The setting or location\n" +
                    "- What should happen in this scene\n" +
                    "- The mood or tone you want\n\n" +
                    "Example: 'Generate a tense scene where Detective Sarah confronts the suspect in an abandoned warehouse at night.'";
            }
            
            logger.info("Generated text length: {}, isEmpty: {}", 
                generatedText != null ? generatedText.length() : "null",
                generatedText != null ? generatedText.isEmpty() : "null");
            int generatedTokens = countTokens(generatedText);
            
            // Build response
            return GenerationResponse.builder()
                .generationId(UUID.randomUUID().toString())
                .contextId(request.getContextId())
                .generationType(request.getGenerationType())
                .generatedText(generatedText)
                .tokenCount(generatedTokens)
                .promptTokenCount(promptTokens)
                .totalTokenCount(promptTokens + generatedTokens)
                .generationTimeMs(System.currentTimeMillis() - startTime)
                .modelUsed(modelName)
                .metrics(buildMetrics(promptTokens, generatedTokens, System.currentTimeMillis() - startTime))
                .safetyRatings(extractSafetyRatings(response))
                .timestamp(Instant.now())
                .truncated(false)
                .confidenceScore(0.85) // TODO: Calculate actual confidence
                .build();
                
        } catch (IOException e) {
            logger.error("Failed to generate text", e);
            throw new GenerationException("Failed to generate text: " + e.getMessage(), e);
        }
    }
    
    @Override
    public CompletableFuture<Void> streamText(GenerationRequest request, StreamCallback callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                String modelName = selectModel(request);
                GenerativeModel model = new GenerativeModel(modelName, vertexAI);
                
                GenerativeModel configuredModel = model.withGenerationConfig(buildGenerationConfig(request.getParameters()))
                    .withSafetySettings(buildSafetySettings(request.getParameters().getSafetyLevel()));
                
                String enhancedPrompt = buildEnhancedPrompt(request);
                Content content = ContentMaker.fromString(enhancedPrompt);
                
                // Stream generation
                var responseStream = configuredModel.generateContentStream(content);
                
                responseStream.forEach(partialResponse -> {
                    String chunk = ResponseHandler.getText(partialResponse);
                    callback.onChunk(chunk);
                });
                
                callback.onComplete();
                
            } catch (Exception e) {
                logger.error("Failed to stream text", e);
                callback.onError(e);
            }
        });
    }
    
    @Override
    public boolean validatePrompt(String prompt) {
        // TODO: Implement prompt validation using Gemini's safety filters
        return prompt != null && !prompt.trim().isEmpty() && prompt.length() < 100000;
    }
    
    @Override
    public int countTokens(String text) {
        // Rough estimation: ~0.75 tokens per character for English text
        // TODO: Use actual tokenizer
        return (int)(text.length() * 0.75);
    }
    
    private String selectModel(GenerationRequest request) {
        // First check model preference
        if (request.getParameters() != null && request.getParameters().getModelPreference() != null) {
            return request.getParameters().getModelPreference().getModelName();
        }
        
        // Otherwise use generation type recommendation
        return request.getGenerationType().getRecommendedModel();
    }
    
    private GenerationConfig buildGenerationConfig(GenerationParameters params) {
        return GenerationConfig.newBuilder()
            .setTemperature((float) params.getTemperature())
            .setMaxOutputTokens(params.getMaxTokens())
            .setTopK(params.getTopK())
            .setTopP((float) params.getTopP())
            .setCandidateCount(params.getCandidateCount())
            .build();
    }
    
    private List<SafetySetting> buildSafetySettings(GenerationParameters.SafetyLevel level) {
        SafetySetting.HarmBlockThreshold threshold = switch (level) {
            case MINIMAL -> SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH;
            case MODERATE -> SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE;
            case STRICT -> SafetySetting.HarmBlockThreshold.BLOCK_LOW_AND_ABOVE;
        };
        
        return Arrays.asList(
            SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                .setThreshold(threshold)
                .build(),
            SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                .setThreshold(threshold)
                .build(),
            SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                .setThreshold(threshold)
                .build(),
            SafetySetting.newBuilder()
                .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                .setThreshold(threshold)
                .build()
        );
    }
    
    private String buildEnhancedPrompt(GenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        // Add system instructions based on generation type
        prompt.append(getSystemInstructions(request)).append("\n\n");
        
        // Add style guide if present
        if (request.getStyleGuide() != null) {
            prompt.append(buildStyleInstructions(request.getStyleGuide())).append("\n\n");
        }
        
        // Add character context if present
        if (request.getCharacters() != null && !request.getCharacters().isEmpty()) {
            prompt.append(buildCharacterContext(request.getCharacters())).append("\n\n");
        }
        
        // Add previous context if present
        if (request.getPreviousContext() != null && !request.getPreviousContext().isEmpty()) {
            prompt.append("Previous context:\n");
            request.getPreviousContext().forEach(ctx -> prompt.append(ctx).append("\n"));
            prompt.append("\n");
        }
        
        // Add the main prompt
        prompt.append("Request: ").append(request.getPrompt());
        
        return prompt.toString();
    }
    
    private String getSystemInstructions(GenerationRequest request) {
        String baseInstructions = String.format(
            "You are an AI writing assistant helping to create a novel. " +
            "Your task is to %s. " +
            "Write in a style appropriate for %s fiction. " +
            "Maintain consistency with the established narrative and characters.",
            request.getGenerationType().getDescription(),
            request.getStyleGuide() != null ? request.getStyleGuide().getGenre() : "general"
        );
        
        // Add specific instructions for scene generation
        if (request.getGenerationType() == GenerationType.SCENE) {
            baseInstructions += "\n\nFor scene generation:\n" +
                "- Create vivid, engaging narrative prose\n" +
                "- Include dialogue, action, and description\n" +
                "- Show character emotions and motivations\n" +
                "- Use sensory details to bring the scene to life\n" +
                "- Aim for 500-1000 words unless specified otherwise\n" +
                "- Begin the scene with an engaging hook\n" +
                "- End with a compelling transition or cliffhanger";
        }
        
        return baseInstructions;
    }
    
    private String buildStyleInstructions(StyleGuide style) {
        return String.format(
            "Style Guidelines:\n" +
            "- Genre: %s\n" +
            "- Point of View: %s\n" +
            "- Tense: %s\n" +
            "- Tone: %s\n" +
            "- Pacing: %s\n" +
            "- Description Level: %s\n" +
            "%s",
            style.getGenre(),
            style.getPointOfView(),
            style.getTense(),
            style.getTone() != null ? style.getTone() : "appropriate to scene",
            style.getPacing(),
            style.getDescriptionLevel(),
            style.getCustomInstructions() != null ? "Additional: " + style.getCustomInstructions() : ""
        );
    }
    
    private String buildCharacterContext(List<CharacterContext> characters) {
        StringBuilder context = new StringBuilder("Characters in this scene:\n");
        
        for (CharacterContext character : characters) {
            context.append(String.format(
                "\n%s: %s\n" +
                "- Personality: %s\n" +
                "- Voice: %s\n" +
                "- Current mood: %s\n",
                character.getName(),
                character.getDescription(),
                character.getPersonality(),
                character.getVoice(),
                character.getCurrentMood()
            ));
        }
        
        return context.toString();
    }
    
    private GenerationResponse.GenerationMetrics buildMetrics(int promptTokens, int generatedTokens, long timeMs) {
        // Rough cost estimation (varies by model)
        double costPerMillionTokens = 2.50; // Example rate
        double estimatedCost = ((promptTokens + generatedTokens) / 1_000_000.0) * costPerMillionTokens;
        
        return GenerationResponse.GenerationMetrics.builder()
            .estimatedCost(estimatedCost)
            .contextWindowUsage((promptTokens + generatedTokens) / 128000.0) // Assuming 128k context
            .memoryItemsRetrieved(0) // TODO: Implement when memory service is integrated
            .coherenceScore(0.85) // TODO: Implement scoring
            .relevanceScore(0.90)
            .creativityScore(0.80)
            .latency(GenerationResponse.GenerationMetrics.LatencyBreakdown.builder()
                .memoryRetrievalMs(0)
                .promptConstructionMs(50)
                .modelInferenceMs(timeMs - 50)
                .postProcessingMs(0)
                .build())
            .build();
    }
    
    private GenerationResponse.SafetyRatings extractSafetyRatings(GenerateContentResponse response) {
        // TODO: Extract actual safety ratings from response
        return GenerationResponse.SafetyRatings.builder()
            .violence("NEGLIGIBLE")
            .sexual("NEGLIGIBLE")
            .harmful("NEGLIGIBLE")
            .harassment("NEGLIGIBLE")
            .overallScore(0.95)
            .contentBlocked(false)
            .build();
    }
}