package com.deusexmachina.auth.repository.impl;

import com.deusexmachina.auth.domain.Session;
import com.deusexmachina.auth.repository.SessionRepository;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SessionRepository.
 * For production, this should be replaced with Cloud SQL implementation.
 * Follows Liskov Substitution Principle - can be swapped with any SessionRepository implementation.
 */
@Singleton
public class InMemorySessionRepository implements SessionRepository {
    private static final Logger logger = LoggerFactory.getLogger(InMemorySessionRepository.class);
    
    private final Map<String, Session> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionsByTokenHash = new ConcurrentHashMap<>();
    private final Map<String, List<String>> sessionsByUserId = new ConcurrentHashMap<>();
    
    @Override
    public CompletableFuture<Session> create(Session session) {
        return CompletableFuture.supplyAsync(() -> {
            sessionsById.put(session.getSessionId(), session);
            sessionsByTokenHash.put(session.getRefreshTokenHash(), session);
            
            sessionsByUserId.computeIfAbsent(session.getUserId(), k -> new ArrayList<>())
                    .add(session.getSessionId());
            
            logger.info("Created session {} for user {}", session.getSessionId(), session.getUserId());
            return session;
        });
    }
    
    @Override
    public CompletableFuture<Optional<Session>> findById(String sessionId) {
        return CompletableFuture.supplyAsync(() -> 
                Optional.ofNullable(sessionsById.get(sessionId)));
    }
    
    @Override
    public CompletableFuture<Optional<Session>> findByRefreshTokenHash(String tokenHash) {
        return CompletableFuture.supplyAsync(() -> 
                Optional.ofNullable(sessionsByTokenHash.get(tokenHash)));
    }
    
    @Override
    public CompletableFuture<List<Session>> findActiveByUserId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> sessionIds = sessionsByUserId.getOrDefault(userId, Collections.emptyList());
            
            return sessionIds.stream()
                    .map(sessionsById::get)
                    .filter(Objects::nonNull)
                    .filter(Session::isActive)
                    .collect(Collectors.toList());
        });
    }
    
    @Override
    public CompletableFuture<Session> updateLastAccessed(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            Session session = sessionsById.get(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("Session not found: " + sessionId);
            }
            
            Session updatedSession = session.toBuilder()
                    .lastAccessedAt(Instant.now())
                    .build();
            
            sessionsById.put(sessionId, updatedSession);
            sessionsByTokenHash.put(updatedSession.getRefreshTokenHash(), updatedSession);
            
            return updatedSession;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> revoke(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            Session session = sessionsById.get(sessionId);
            if (session == null) {
                return false;
            }
            
            Session revokedSession = session.toBuilder()
                    .revokedAt(Instant.now())
                    .build();
            
            sessionsById.put(sessionId, revokedSession);
            sessionsByTokenHash.remove(session.getRefreshTokenHash());
            
            logger.info("Revoked session {}", sessionId);
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Integer> revokeAllForUser(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> sessionIds = sessionsByUserId.getOrDefault(userId, Collections.emptyList());
            int count = 0;
            
            for (String sessionId : sessionIds) {
                Session session = sessionsById.get(sessionId);
                if (session != null && session.isActive()) {
                    Session revokedSession = session.toBuilder()
                            .revokedAt(Instant.now())
                            .build();
                    
                    sessionsById.put(sessionId, revokedSession);
                    sessionsByTokenHash.remove(session.getRefreshTokenHash());
                    count++;
                }
            }
            
            logger.info("Revoked {} sessions for user {}", count, userId);
            return count;
        });
    }
    
    @Override
    public CompletableFuture<Integer> deleteExpired() {
        return CompletableFuture.supplyAsync(() -> {
            Instant now = Instant.now();
            int count = 0;
            
            Iterator<Map.Entry<String, Session>> iterator = sessionsById.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Session> entry = iterator.next();
                Session session = entry.getValue();
                
                if (session.isExpired() || 
                    (session.getRevokedAt() != null && 
                     session.getRevokedAt().isBefore(now.minus(java.time.Duration.ofDays(7))))) {
                    
                    iterator.remove();
                    sessionsByTokenHash.remove(session.getRefreshTokenHash());
                    
                    List<String> userSessions = sessionsByUserId.get(session.getUserId());
                    if (userSessions != null) {
                        userSessions.remove(session.getSessionId());
                    }
                    
                    count++;
                }
            }
            
            logger.info("Deleted {} expired sessions", count);
            return count;
        });
    }
}