package com.deusexmachina.novel.memory.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Request model for creating a new character.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCharacterRequest {
    private String projectId;
    private String name;
    private String role; // protagonist, antagonist, supporting, minor
    private String backstory;
    private List<String> goals;
    private List<String> motivations;
    private List<String> conflicts;
    private Map<String, Object> attributes; // Physical and personality traits
    private String voiceProfile;
    private List<String> speechPatterns;
    private Map<String, String> relationships; // Initial relationships with other characters
}