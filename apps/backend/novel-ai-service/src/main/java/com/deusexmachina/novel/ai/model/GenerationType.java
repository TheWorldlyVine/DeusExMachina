package com.deusexmachina.novel.ai.model;

public enum GenerationType {
    SCENE("scene", "Generate a complete scene with dialogue and action"),
    CHAPTER("chapter", "Generate a full chapter with multiple scenes"),
    DIALOGUE("dialogue", "Generate character dialogue and interactions"),
    DESCRIPTION("description", "Generate descriptive passages"),
    ACTION("action", "Generate action sequences"),
    TRANSITION("transition", "Generate transitions between scenes"),
    OUTLINE("outline", "Generate plot outlines or summaries"),
    CHARACTER_THOUGHT("character_thought", "Generate internal monologue or thoughts"),
    WORLDBUILDING("worldbuilding", "Generate world details and lore");
    
    private final String value;
    private final String description;
    
    GenerationType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static GenerationType fromValue(String value) {
        for (GenerationType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown generation type: " + value);
    }
}