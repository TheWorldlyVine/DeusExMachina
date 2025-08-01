package com.deusexmachina.auth.service.impl;

import com.deusexmachina.auth.domain.Permission;
import com.deusexmachina.auth.domain.PermissionLevel;
import com.deusexmachina.auth.dto.PermissionGrantRequest;
import com.deusexmachina.auth.exception.AuthException;
import com.deusexmachina.auth.repository.PermissionRepository;
import com.deusexmachina.auth.service.AuthorizationService;
import com.google.cloud.Timestamp;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of AuthorizationService.
 * Handles permission checks and management.
 */
@Singleton
public class AuthorizationServiceImpl implements AuthorizationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);
    
    private final PermissionRepository permissionRepository;
    
    @Inject
    public AuthorizationServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }
    
    @Override
    public CompletableFuture<Boolean> checkPermission(String userId, String resourceId, String action) {
        return permissionRepository.findByUserAndResource(userId, resourceId)
                .thenApply(optionalPermission -> {
                    if (optionalPermission.isEmpty()) {
                        return false;
                    }
                    
                    Permission permission = optionalPermission.get();
                    if (permission.isExpired()) {
                        return false;
                    }
                    
                    return permission.hasPermission(action);
                });
    }
    
    @Override
    public CompletableFuture<Boolean> hasPermissionLevel(String userId, String resourceId, 
                                                         PermissionLevel requiredLevel) {
        return permissionRepository.findHighestPermissionLevel(userId, resourceId)
                .thenApply(optionalLevel -> {
                    if (optionalLevel.isEmpty()) {
                        return false;
                    }
                    
                    PermissionLevel userLevel = optionalLevel.get();
                    return userLevel.hasPrivilegesOf(requiredLevel);
                });
    }
    
    @Override
    public CompletableFuture<String> grantPermission(PermissionGrantRequest request) {
        // First check if granting user has permission to share
        return checkPermission(request.grantedBy(), request.resourceId(), "share")
                .thenCompose(canShare -> {
                    if (!canShare) {
                        throw new AuthException("User does not have permission to share this resource", 403);
                    }
                    
                    // Check if user already has a permission
                    return permissionRepository.findByUserAndResource(request.grantedTo(), request.resourceId());
                })
                .thenCompose(existingPermission -> {
                    Permission permission;
                    
                    if (existingPermission.isPresent()) {
                        // Update existing permission
                        Permission existing = existingPermission.get();
                        permission = Permission.builder()
                                .permissionId(existing.getPermissionId())
                                .resourceId(request.resourceId())
                                .resourceType(request.resourceType())
                                .grantedTo(request.grantedTo())
                                .grantedBy(request.grantedBy())
                                .level(request.level())
                                .grantedAt(existing.getGrantedAt())
                                .expiresAt(request.expiresAt() != null ? Timestamp.ofTimeSecondsAndNanos(request.expiresAt().getEpochSecond(), request.expiresAt().getNano()) : null)
                                .customPermissions(request.customPermissions())
                                .build();
                        
                        return permissionRepository.update(permission);
                    } else {
                        // Create new permission
                        permission = Permission.builder()
                                .permissionId(UUID.randomUUID().toString())
                                .resourceId(request.resourceId())
                                .resourceType(request.resourceType())
                                .grantedTo(request.grantedTo())
                                .grantedBy(request.grantedBy())
                                .level(request.level())
                                .expiresAt(request.expiresAt() != null ? Timestamp.ofTimeSecondsAndNanos(request.expiresAt().getEpochSecond(), request.expiresAt().getNano()) : null)
                                .customPermissions(request.customPermissions())
                                .build();
                        
                        return permissionRepository.create(permission);
                    }
                })
                .thenApply(permission -> {
                    logger.info("Permission granted: {} to user {} for resource {} with level {}", 
                            permission.getPermissionId(), request.grantedTo(), 
                            request.resourceId(), request.level());
                    return permission.getPermissionId();
                });
    }
    
    @Override
    public CompletableFuture<Boolean> revokePermission(String permissionId, String revokedBy) {
        return permissionRepository.findById(permissionId)
                .thenCompose(optionalPermission -> {
                    if (optionalPermission.isEmpty()) {
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    Permission permission = optionalPermission.get();
                    
                    // Check if revoking user has permission
                    return checkPermission(revokedBy, permission.getResourceId(), "share")
                            .thenCompose(canRevoke -> {
                                if (!canRevoke && !permission.getGrantedBy().equals(revokedBy)) {
                                    throw new AuthException("User does not have permission to revoke", 403);
                                }
                                
                                return permissionRepository.deleteById(permissionId);
                            });
                });
    }
    
    @Override
    public CompletableFuture<Integer> revokeAllPermissions(String userId, String resourceId) {
        return permissionRepository.deleteByUserAndResource(userId, resourceId)
                .thenApply(count -> {
                    logger.info("Revoked {} permissions for user {} on resource {}", 
                            count, userId, resourceId);
                    return count;
                });
    }
    
    @Override
    public CompletableFuture<List<Permission>> getResourcePermissions(String resourceId) {
        return permissionRepository.findByResourceId(resourceId);
    }
    
    @Override
    public CompletableFuture<List<Permission>> getUserPermissions(String userId) {
        return permissionRepository.findByUserId(userId);
    }
    
    @Override
    public CompletableFuture<PermissionLevel> getEffectivePermissionLevel(String userId, String resourceId) {
        return permissionRepository.findHighestPermissionLevel(userId, resourceId)
                .thenApply(optionalLevel -> optionalLevel.orElse(null));
    }
    
    @Override
    public CompletableFuture<Boolean> transferOwnership(String resourceId, String fromUserId, String toUserId) {
        // First verify current owner
        return permissionRepository.findByUserAndResource(fromUserId, resourceId)
                .thenCompose(optionalPermission -> {
                    if (optionalPermission.isEmpty() || 
                        optionalPermission.get().getLevel() != PermissionLevel.OWNER) {
                        throw new AuthException("User is not the owner of the resource", 403);
                    }
                    
                    Permission currentOwnership = optionalPermission.get();
                    
                    // Revoke current owner permission
                    return permissionRepository.deleteById(currentOwnership.getPermissionId())
                            .thenCompose(deleted -> {
                                // Grant ownership to new user
                                Permission newOwnership = Permission.builder()
                                        .permissionId(UUID.randomUUID().toString())
                                        .resourceId(resourceId)
                                        .resourceType(currentOwnership.getResourceType())
                                        .grantedTo(toUserId)
                                        .grantedBy(fromUserId)
                                        .level(PermissionLevel.OWNER)
                                        .build();
                                
                                return permissionRepository.create(newOwnership);
                            })
                            .thenApply(permission -> {
                                logger.info("Ownership transferred for resource {} from {} to {}", 
                                        resourceId, fromUserId, toUserId);
                                return true;
                            });
                });
    }
}