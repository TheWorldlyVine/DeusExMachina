package com.deusexmachina.auth.config;

import com.deusexmachina.auth.repository.SessionRepository;
import com.deusexmachina.auth.repository.UserRepository;
import com.deusexmachina.auth.repository.impl.FirestoreUserRepository;
import com.deusexmachina.auth.service.*;
import com.deusexmachina.auth.service.impl.*;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import java.io.IOException;

/**
 * Guice module for dependency injection configuration.
 * Follows Dependency Inversion Principle - binds interfaces to implementations.
 */
public class AuthModule extends AbstractModule {
    
    @Override
    protected void configure() {
        // Bind repositories
        bind(UserRepository.class).to(FirestoreUserRepository.class);
        bind(SessionRepository.class).to(com.deusexmachina.auth.repository.impl.InMemorySessionRepository.class);
        bind(com.deusexmachina.auth.repository.PermissionRepository.class)
                .to(com.deusexmachina.auth.repository.impl.InMemoryPermissionRepository.class);
        
        // Bind services
        bind(AuthenticationService.class).to(AuthenticationServiceImpl.class);
        bind(PasswordService.class).to(PasswordServiceImpl.class);
        bind(TokenService.class).to(TokenServiceImpl.class);
        // Use CloudPubSubEmailService for scalable email delivery
        bind(EmailService.class).to(com.deusexmachina.email.service.CloudPubSubEmailService.class);
        bind(com.deusexmachina.auth.service.AuthorizationService.class)
                .to(com.deusexmachina.auth.service.impl.AuthorizationServiceImpl.class);
        
        // Bind configuration values
        bindConfiguration();
    }
    
    private void bindConfiguration() {
        // Email configuration (Note: SendGrid removed, using Cloud Pub/Sub instead)
        bind(String.class).annotatedWith(Names.named("email.from.address"))
                .toInstance(getEnvOrDefault("EMAIL_FROM_ADDRESS", "noreply@deusexmachina.com"));
        bind(String.class).annotatedWith(Names.named("email.from.name"))
                .toInstance(getEnvOrDefault("EMAIL_FROM_NAME", "DeusExMachina"));
        bind(String.class).annotatedWith(Names.named("app.base.url"))
                .toInstance(getEnvOrDefault("APP_BASE_URL", "https://app.deusexmachina.com"));
        
        // Google OAuth configuration
        bind(String.class).annotatedWith(Names.named("google.client.id"))
                .toInstance(getEnvOrDefault("GOOGLE_CLIENT_ID", ""));
        
        // Pub/Sub configuration
        String projectId = System.getenv("GCP_PROJECT_ID");
        if (projectId == null || projectId.isEmpty()) {
            projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        }
        if (projectId == null || projectId.isEmpty()) {
            projectId = "deus-ex-machina-prod"; // Fallback
        }
        bind(String.class).annotatedWith(Names.named("gcp.project.id"))
                .toInstance(projectId);
        bind(String.class).annotatedWith(Names.named("pubsub.topic.email-events"))
                .toInstance(getEnvOrDefault("EMAIL_TOPIC_NAME", "deus-ex-machina-email-events"));
        bind(String.class).annotatedWith(Names.named("service.name"))
                .toInstance("auth-function");
    }
    
    @Provides
    @Singleton
    Firestore provideFirestore(@Named("gcp.project.id") String projectId) {
        return FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }
    
    @Provides
    @Singleton
    SecretManagerServiceClient provideSecretManager() {
        try {
            return SecretManagerServiceClient.create();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create SecretManager client", e);
        }
    }
    
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue != null ? defaultValue : "";
    }
}