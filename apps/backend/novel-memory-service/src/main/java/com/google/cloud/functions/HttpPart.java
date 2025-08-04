package com.google.cloud.functions;

import java.io.InputStream;
import java.util.Optional;

/**
 * Minimal HttpPart interface for Cloud Run compatibility
 */
public interface HttpPart {
    Optional<String> getFileName();
    Optional<String> getContentType();
    InputStream getInputStream();
}