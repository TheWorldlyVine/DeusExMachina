package com.deusexmachina.auth.repository;

import com.deusexmachina.auth.domain.User;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for User persistence.
 * Following Interface Segregation Principle - focused on user data operations.
 */
public interface UserRepository {
    /**
     * Find a user by their unique ID.
     */
    CompletableFuture<Optional<User>> findById(String userId);
    
    /**
     * Find a user by their email address.
     */
    CompletableFuture<Optional<User>> findByEmail(String email);
    
    /**
     * Save a user (create or update).
     */
    CompletableFuture<User> save(User user);
    
    /**
     * Delete a user by ID.
     */
    CompletableFuture<Boolean> deleteById(String userId);
    
    /**
     * Check if a user exists with the given email.
     */
    CompletableFuture<Boolean> existsByEmail(String email);
    
    /**
     * Update specific fields of a user.
     */
    CompletableFuture<User> update(String userId, UserUpdateRequest updateRequest);
    
    /**
     * Update user security settings.
     */
    CompletableFuture<User> updateSecuritySettings(String userId, 
            com.deusexmachina.auth.domain.UserSecuritySettings settings);
    
    /**
     * Record interface for partial updates.
     */
    record UserUpdateRequest(
            String displayName,
            Boolean emailVerified,
            String passwordHash,
            com.deusexmachina.auth.domain.UserSecuritySettings securitySettings
    ) {}
}