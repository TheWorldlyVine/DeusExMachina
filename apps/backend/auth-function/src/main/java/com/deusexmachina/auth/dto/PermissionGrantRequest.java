package com.deusexmachina.auth.dto;

import com.deusexmachina.auth.domain.PermissionLevel;
import com.deusexmachina.auth.domain.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for permission grant request.
 */
public record PermissionGrantRequest(
    @NotBlank(message = "Resource ID is required")
    String resourceId,
    
    @NotNull(message = "Resource type is required")
    ResourceType resourceType,
    
    @NotBlank(message = "User ID to grant to is required")
    String grantedTo,
    
    @NotBlank(message = "Granting user ID is required")
    String grantedBy,
    
    @NotNull(message = "Permission level is required")
    PermissionLevel level,
    
    Instant expiresAt,
    
    Map<String, Boolean> customPermissions
) {}