package com.deusexmachina.auth.repository;

import com.deusexmachina.auth.domain.Session;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for Session persistence.
 * Handles JWT refresh tokens and session management.
 */
public interface SessionRepository {
    /**
     * Create a new session.
     */
    CompletableFuture<Session> create(Session session);
    
    /**
     * Find a session by its ID.
     */
    CompletableFuture<Optional<Session>> findById(String sessionId);
    
    /**
     * Find a session by refresh token hash.
     */
    CompletableFuture<Optional<Session>> findByRefreshTokenHash(String tokenHash);
    
    /**
     * Find all active sessions for a user.
     */
    CompletableFuture<List<Session>> findActiveByUserId(String userId);
    
    /**
     * Update session last accessed time.
     */
    CompletableFuture<Session> updateLastAccessed(String sessionId);
    
    /**
     * Revoke a session.
     */
    CompletableFuture<Boolean> revoke(String sessionId);
    
    /**
     * Revoke all sessions for a user.
     */
    CompletableFuture<Integer> revokeAllForUser(String userId);
    
    /**
     * Clean up expired sessions.
     */
    CompletableFuture<Integer> deleteExpired();
}