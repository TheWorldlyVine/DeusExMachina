package com.deusexmachina.novel.ai.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Style guidelines for text generation.
 */
@Data
@Builder
public class StyleGuide {
    
    /**
     * Writing style (e.g., "literary fiction", "thriller", "romance").
     */
    private String genre;
    
    /**
     * Narrative voice (first person, third person limited, omniscient).
     */
    @Builder.Default
    private String pointOfView = "third person limited";
    
    /**
     * Tense (past, present).
     */
    @Builder.Default
    private String tense = "past";
    
    /**
     * Tone (e.g., "dark", "humorous", "serious", "light-hearted").
     */
    private String tone;
    
    /**
     * Pacing preference (fast, moderate, slow).
     */
    @Builder.Default
    private String pacing = "moderate";
    
    /**
     * Description density (minimal, balanced, rich).
     */
    @Builder.Default
    private String descriptionLevel = "balanced";
    
    /**
     * Dialogue style preferences.
     */
    private DialogueStyle dialogueStyle;
    
    /**
     * Specific authors to emulate (optional).
     */
    private List<String> influencedBy;
    
    /**
     * Custom style instructions.
     */
    private String customInstructions;
    
    /**
     * Forbidden elements or topics.
     */
    private List<String> avoid;
    
    /**
     * Target audience (e.g., "young adult", "adult", "literary").
     */
    private String targetAudience;
    
    @Data
    @Builder
    public static class DialogueStyle {
        /**
         * How much dialogue vs. narrative.
         */
        @Builder.Default
        private String balance = "balanced";
        
        /**
         * Formality level.
         */
        @Builder.Default
        private String formality = "natural";
        
        /**
         * Use of dialect or accents.
         */
        @Builder.Default
        private boolean useDialect = false;
        
        /**
         * Attribution style (said, action beats, minimal).
         */
        @Builder.Default
        private String attributionStyle = "varied";
    }
}