package com.deusexmachina.novel.memory.model;

public enum MemoryType {
    STATE("state", "Current state of entities"),
    CONTEXT("context", "Contextual information and setting"),
    OBSERVATION("observation", "Observed facts and events"),
    REFLECTION("reflection", "Character thoughts and reflections"),
    EXECUTION("execution", "Actions taken and their outcomes");
    
    private final String value;
    private final String description;
    
    MemoryType(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MemoryType fromValue(String value) {
        for (MemoryType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown memory type: " + value);
    }
}