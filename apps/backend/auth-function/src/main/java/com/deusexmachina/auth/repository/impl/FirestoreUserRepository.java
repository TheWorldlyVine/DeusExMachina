package com.deusexmachina.auth.repository.impl;

import com.deusexmachina.auth.domain.User;
import com.deusexmachina.auth.domain.UserSecuritySettings;
import com.deusexmachina.auth.repository.UserRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of UserRepository.
 * Follows Dependency Inversion Principle - depends on abstraction (UserRepository).
 */
@Singleton
public class FirestoreUserRepository implements UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreUserRepository.class);
    private static final String COLLECTION_NAME = "users";
    
    private final Firestore firestore;
    
    @Inject
    public FirestoreUserRepository(Firestore firestore) {
        this.firestore = firestore;
    }
    
    @Override
    public CompletableFuture<Optional<User>> findById(String userId) {
        return executeAsync(() -> {
            DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                    .document(userId)
                    .get()
                    .get();
            
            if (document.exists()) {
                return Optional.of(documentToUser(document));
            }
            return Optional.empty();
        });
    }
    
    @Override
    public CompletableFuture<Optional<User>> findByEmail(String email) {
        return executeAsync(() -> {
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();
            
            if (!querySnapshot.isEmpty()) {
                return Optional.of(documentToUser(querySnapshot.getDocuments().get(0)));
            }
            return Optional.empty();
        });
    }
    
    @Override
    public CompletableFuture<User> save(User user) {
        return executeAsync(() -> {
            String userId = user.getUserId() != null ? user.getUserId() : UUID.randomUUID().toString();
            
            Map<String, Object> data = userToMap(user);
            data.put("updatedAt", FieldValue.serverTimestamp());
            
            if (user.getUserId() == null) {
                // New user
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("userId", userId);
            }
            
            firestore.collection(COLLECTION_NAME)
                    .document(userId)
                    .set(data, SetOptions.merge())
                    .get();
            
            // Return the saved user with generated ID
            return user.toBuilder()
                    .userId(userId)
                    .updatedAt(Timestamp.now())
                    .build();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteById(String userId) {
        return executeAsync(() -> {
            firestore.collection(COLLECTION_NAME)
                    .document(userId)
                    .delete()
                    .get();
            return true;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> existsByEmail(String email) {
        return executeAsync(() -> {
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();
            
            return !querySnapshot.isEmpty();
        });
    }
    
    @Override
    public CompletableFuture<User> update(String userId, UserUpdateRequest updateRequest) {
        return executeAsync(() -> {
            Map<String, Object> updates = new HashMap<>();
            
            if (updateRequest.displayName() != null) {
                updates.put("displayName", updateRequest.displayName());
            }
            if (updateRequest.emailVerified() != null) {
                updates.put("emailVerified", updateRequest.emailVerified());
            }
            if (updateRequest.passwordHash() != null) {
                updates.put("passwordHash", updateRequest.passwordHash());
            }
            if (updateRequest.securitySettings() != null) {
                updates.put("securitySettings", securitySettingsToMap(updateRequest.securitySettings()));
            }
            
            updates.put("updatedAt", FieldValue.serverTimestamp());
            
            firestore.collection(COLLECTION_NAME)
                    .document(userId)
                    .update(updates)
                    .get();
            
            // Fetch and return updated user
            return findById(userId).get().orElseThrow(() -> 
                    new IllegalStateException("User not found after update"));
        });
    }
    
    @Override
    public CompletableFuture<User> updateSecuritySettings(String userId, UserSecuritySettings settings) {
        return update(userId, new UserUpdateRequest(null, null, null, settings));
    }
    
    private User documentToUser(DocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        if (data == null) {
            throw new IllegalStateException("Document data is null");
        }
        
        return User.builder()
                .userId(document.getId())
                .email((String) data.get("email"))
                .passwordHash((String) data.get("passwordHash"))
                .displayName((String) data.get("displayName"))
                .authProvider(com.deusexmachina.auth.domain.AuthProvider.fromValue(
                        (String) data.get("authProvider")))
                .emailVerified((Boolean) data.getOrDefault("emailVerified", false))
                .createdAt((Timestamp) data.get("createdAt"))
                .updatedAt((Timestamp) data.get("updatedAt"))
                .linkedProviders((java.util.List<String>) data.getOrDefault("linkedProviders", 
                        new java.util.ArrayList<>()))
                .securitySettings(mapToSecuritySettings(
                        (Map<String, Object>) data.get("securitySettings")))
                .build();
    }
    
    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", user.getEmail());
        map.put("passwordHash", user.getPasswordHash());
        map.put("displayName", user.getDisplayName());
        map.put("authProvider", user.getAuthProvider().getValue());
        map.put("emailVerified", user.isEmailVerified());
        map.put("linkedProviders", user.getLinkedProviders());
        map.put("securitySettings", securitySettingsToMap(user.getSecuritySettings()));
        
        if (user.getCreatedAt() != null) {
            map.put("createdAt", user.getCreatedAt());
        }
        if (user.getUpdatedAt() != null) {
            map.put("updatedAt", user.getUpdatedAt());
        }
        
        return map;
    }
    
    private Map<String, Object> securitySettingsToMap(UserSecuritySettings settings) {
        if (settings == null) return null;
        
        Map<String, Object> map = new HashMap<>();
        map.put("mfaEnabled", settings.isMfaEnabled());
        map.put("mfaSecret", settings.getMfaSecret());
        map.put("lastPasswordChange", settings.getLastPasswordChange());
        map.put("failedLoginAttempts", settings.getFailedLoginAttempts());
        map.put("lockoutUntil", settings.getLockoutUntil());
        return map;
    }
    
    private UserSecuritySettings mapToSecuritySettings(Map<String, Object> map) {
        if (map == null) return new UserSecuritySettings();
        
        return UserSecuritySettings.builder()
                .mfaEnabled((Boolean) map.getOrDefault("mfaEnabled", false))
                .mfaSecret((String) map.get("mfaSecret"))
                .lastPasswordChange((Timestamp) map.get("lastPasswordChange"))
                .failedLoginAttempts(((Long) map.getOrDefault("failedLoginAttempts", 0L)).intValue())
                .lockoutUntil((Timestamp) map.get("lockoutUntil"))
                .build();
    }
    
    private <T> CompletableFuture<T> executeAsync(ThrowingSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                logger.error("Firestore operation failed", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}