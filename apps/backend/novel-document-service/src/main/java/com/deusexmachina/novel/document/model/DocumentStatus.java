package com.deusexmachina.novel.document.model;

public enum DocumentStatus {
    DRAFT("draft", "Work in progress"),
    IN_REVIEW("in_review", "Under review"),
    PUBLISHED("published", "Published and available"),
    ARCHIVED("archived", "Archived document");
    
    private final String value;
    private final String description;
    
    DocumentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static DocumentStatus fromValue(String value) {
        for (DocumentStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown document status: " + value);
    }
}