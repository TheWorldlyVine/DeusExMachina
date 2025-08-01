package com.deusexmachina.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for permission check request.
 */
public record PermissionCheckRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Resource ID is required")
    String resourceId,
    
    @NotBlank(message = "Action is required")
    String action
) {}