package com.deusexmachina.auth.service;

import com.deusexmachina.auth.domain.PermissionLevel;
import com.deusexmachina.auth.domain.ResourceType;
import com.deusexmachina.auth.dto.PermissionCheckRequest;
import com.deusexmachina.auth.dto.PermissionGrantRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for authorization operations.
 * Single Responsibility: Handle permission checks and grants.
 */
public interface AuthorizationService {
    /**
     * Check if a user has permission to perform an action on a resource.
     */
    CompletableFuture<Boolean> checkPermission(String userId, String resourceId, String action);
    
    /**
     * Check if a user has a specific permission level on a resource.
     */
    CompletableFuture<Boolean> hasPermissionLevel(String userId, String resourceId, PermissionLevel requiredLevel);
    
    /**
     * Grant permission to a user for a resource.
     */
    CompletableFuture<String> grantPermission(PermissionGrantRequest request);
    
    /**
     * Revoke a specific permission.
     */
    CompletableFuture<Boolean> revokePermission(String permissionId, String revokedBy);
    
    /**
     * Revoke all permissions for a user on a resource.
     */
    CompletableFuture<Integer> revokeAllPermissions(String userId, String resourceId);
    
    /**
     * Get all permissions for a resource.
     */
    CompletableFuture<List<com.deusexmachina.auth.domain.Permission>> getResourcePermissions(String resourceId);
    
    /**
     * Get all permissions granted to a user.
     */
    CompletableFuture<List<com.deusexmachina.auth.domain.Permission>> getUserPermissions(String userId);
    
    /**
     * Get effective permission level for a user on a resource.
     */
    CompletableFuture<PermissionLevel> getEffectivePermissionLevel(String userId, String resourceId);
    
    /**
     * Transfer ownership of a resource.
     */
    CompletableFuture<Boolean> transferOwnership(String resourceId, String fromUserId, String toUserId);
}