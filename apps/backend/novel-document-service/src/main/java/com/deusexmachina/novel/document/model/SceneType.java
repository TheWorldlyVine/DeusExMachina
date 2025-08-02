package com.deusexmachina.novel.document.model;

public enum SceneType {
    NARRATIVE("narrative"),
    DIALOGUE("dialogue"),
    ACTION("action"),
    DESCRIPTION("description"),
    FLASHBACK("flashback"),
    DREAM("dream"),
    MONTAGE("montage");
    
    private final String value;
    
    SceneType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}