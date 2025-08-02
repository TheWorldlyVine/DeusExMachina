package com.deusexmachina.novel.document.dto;

import com.deusexmachina.novel.document.model.DocumentStatus;
import com.deusexmachina.novel.document.model.Document.DocumentSettings;
import lombok.Data;

import java.util.List;

@Data
public class DocumentRequest {
    private String id;
    private String contextId;
    private String title;
    private String subtitle;
    private String description;
    private String authorId;
    private String authorName;
    private String genre;
    private List<String> tags;
    private DocumentStatus status;
    private DocumentSettings settings;
}