package com.deusexmachina.auth.repository.impl;

import com.deusexmachina.auth.domain.Permission;
import com.deusexmachina.auth.domain.PermissionLevel;
import com.deusexmachina.auth.repository.PermissionRepository;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of PermissionRepository.
 * For production, this should be replaced with Firestore implementation.
 */
@Singleton
public class InMemoryPermissionRepository implements PermissionRepository {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryPermissionRepository.class);
    
    private final Map<String, Permission> permissionsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> permissionsByResource = new ConcurrentHashMap<>();
    private final Map<String, List<String>> permissionsByUser = new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<Permission> create(Permission permission) {
        return CompletableFuture.supplyAsync(() -> {
            permissionsById.put(permission.getPermissionId(), permission);
            
            // Index by resource
            permissionsByResource.computeIfAbsent(permission.getResourceId(), k -> new ArrayList<>())
                    .add(permission.getPermissionId());
            
            // Index by user
            permissionsByUser.computeIfAbsent(permission.getGrantedTo(), k -> new ArrayList<>())
                    .add(permission.getPermissionId());
            
            logger.info("Created permission {} for user {} on resource {}", 
                    permission.getPermissionId(), permission.getGrantedTo(), permission.getResourceId());
            
            return permission;
        });
    }
    
    @Override
    public CompletableFuture<Optional<Permission>> findById(String permissionId) {
        return CompletableFuture.supplyAsync(() -> 
                Optional.ofNullable(permissionsById.get(permissionId)));
    }
    
    @Override
    public CompletableFuture<List<Permission>> findByResourceId(String resourceId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> permissionIds = permissionsByResource.getOrDefault(resourceId, Collections.emptyList());
            
            return permissionIds.stream()
                    .map(permissionsById::get)
                    .filter(Objects::nonNull)
                    .filter(p -> !p.isExpired())
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<List<Permission>> findByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> permissionIds = permissionsByUser.getOrDefault(userId, Collections.emptyList());
            
            return permissionIds.stream()
                    .map(permissionsById::get)
                    .filter(Objects::nonNull)
                    .filter(p -> !p.isExpired())
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Optional<Permission>> findByUserAndResource(String userId, String resourceId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> userPermissions = permissionsByUser.getOrDefault(userId, Collections.emptyList());
            
            return userPermissions.stream()
                    .map(permissionsById::get)
                    .filter(Objects::nonNull)
                    .filter(p -> p.getResourceId().equals(resourceId))
                    .filter(p -> !p.isExpired())
                    .findFirst();
        });
    }
    
    @Override
    public CompletableFuture<Permission> update(Permission permission) {
        return CompletableFuture.supplyAsync(() -> {
            Permission existing = permissionsById.get(permission.getPermissionId());
            if (existing == null) {
                throw new IllegalArgumentException("Permission not found: " + permission.getPermissionId());
            }
            
            permissionsById.put(permission.getPermissionId(), permission);
            logger.info("Updated permission {}", permission.getPermissionId());
            
            return permission;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteById(String permissionId) {
        return CompletableFuture.supplyAsync(() -> {
            Permission permission = permissionsById.remove(permissionId);
            if (permission == null) {
                return false;
            }
            
            // Remove from indexes
            List<String> resourcePerms = permissionsByResource.get(permission.getResourceId());
            if (resourcePerms != null) {
                resourcePerms.remove(permissionId);
            }
            
            List<String> userPerms = permissionsByUser.get(permission.getGrantedTo());
            if (userPerms != null) {
                userPerms.remove(permissionId);
            }
            
            logger.info("Deleted permission {}", permissionId);
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Integer> deleteByResourceId(String resourceId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> permissionIds = permissionsByResource.remove(resourceId);
            if (permissionIds == null || permissionIds.isEmpty()) {
                return 0;
            }
            
            int count = 0;
            for (String permissionId : permissionIds) {
                Permission permission = permissionsById.remove(permissionId);
                if (permission != null) {
                    // Remove from user index
                    List<String> userPerms = permissionsByUser.get(permission.getGrantedTo());
                    if (userPerms != null) {
                        userPerms.remove(permissionId);
                    }
                    count++;
                }
            }
            
            logger.info("Deleted {} permissions for resource {}", count, resourceId);
            return count;
        });
    }
    
    @Override
    public CompletableFuture<Integer> deleteByUserAndResource(String userId, String resourceId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> userPermissions = permissionsByUser.getOrDefault(userId, Collections.emptyList());
            int count = 0;
            
            Iterator<String> iterator = userPermissions.iterator();
            while (iterator.hasNext()) {
                String permissionId = iterator.next();
                Permission permission = permissionsById.get(permissionId);
                
                if (permission != null && permission.getResourceId().equals(resourceId)) {
                    permissionsById.remove(permissionId);
                    iterator.remove();
                    
                    // Remove from resource index
                    List<String> resourcePerms = permissionsByResource.get(resourceId);
                    if (resourcePerms != null) {
                        resourcePerms.remove(permissionId);
                    }
                    
                    count++;
                }
            }
            
            logger.info("Deleted {} permissions for user {} on resource {}", count, userId, resourceId);
            return count;
        });
    }
    
    @Override
    public CompletableFuture<Optional<PermissionLevel>> findHighestPermissionLevel(String userId, String resourceId) {
        return findByUserAndResource(userId, resourceId)
                .thenApply(optionalPermission -> 
                        optionalPermission.map(Permission::getLevel));
    }
    
    @Override
    public CompletableFuture<Integer> deleteExpired() {
        return CompletableFuture.supplyAsync(() -> {
            Instant now = Instant.now();
            int count = 0;
            
            Iterator<Map.Entry<String, Permission>> iterator = permissionsById.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Permission> entry = iterator.next();
                Permission permission = entry.getValue();
                
                if (permission.isExpired()) {
                    String permissionId = entry.getKey();
                    iterator.remove();
                    
                    // Remove from indexes
                    List<String> resourcePerms = permissionsByResource.get(permission.getResourceId());
                    if (resourcePerms != null) {
                        resourcePerms.remove(permissionId);
                    }
                    
                    List<String> userPerms = permissionsByUser.get(permission.getGrantedTo());
                    if (userPerms != null) {
                        userPerms.remove(permissionId);
                    }
                    
                    count++;
                }
            }
            
            logger.info("Deleted {} expired permissions", count);
            return count;
        });
    }
}