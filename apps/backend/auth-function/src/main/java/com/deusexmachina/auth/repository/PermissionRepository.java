package com.deusexmachina.auth.repository;

import com.deusexmachina.auth.domain.Permission;
import com.deusexmachina.auth.domain.PermissionLevel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Permission persistence.
 * Following Interface Segregation Principle.
 */
public interface PermissionRepository {
    /**
     * Create a new permission.
     */
    CompletableFuture<Permission> create(Permission permission);
    
    /**
     * Find a permission by ID.
     */
    CompletableFuture<Optional<Permission>> findById(String permissionId);
    
    /**
     * Find all permissions for a resource.
     */
    CompletableFuture<List<Permission>> findByResourceId(String resourceId);
    
    /**
     * Find all permissions granted to a user.
     */
    CompletableFuture<List<Permission>> findByUserId(String userId);
    
    /**
     * Find a specific permission for a user on a resource.
     */
    CompletableFuture<Optional<Permission>> findByUserAndResource(String userId, String resourceId);
    
    /**
     * Update a permission.
     */
    CompletableFuture<Permission> update(Permission permission);
    
    /**
     * Delete a permission by ID.
     */
    CompletableFuture<Boolean> deleteById(String permissionId);
    
    /**
     * Delete all permissions for a resource.
     */
    CompletableFuture<Integer> deleteByResourceId(String resourceId);
    
    /**
     * Delete all permissions for a user on a specific resource.
     */
    CompletableFuture<Integer> deleteByUserAndResource(String userId, String resourceId);
    
    /**
     * Find the highest permission level for a user on a resource.
     */
    CompletableFuture<Optional<PermissionLevel>> findHighestPermissionLevel(String userId, String resourceId);
    
    /**
     * Clean up expired permissions.
     */
    CompletableFuture<Integer> deleteExpired();
}