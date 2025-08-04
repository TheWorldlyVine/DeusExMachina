package com.google.cloud.functions;

import java.util.List;

/**
 * Minimal HttpHeaders interface for Cloud Run compatibility
 */
public interface HttpHeaders {
    List<String> getValues();
}